package io.github.oldmanpushcart.dashscope4j.agent.memory;

import io.github.oldmanpushcart.dashscope4j.agent.memory.store.MemoryStore;
import io.github.oldmanpushcart.dashscope4j.agent.memory.store.MemoryStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 工作记忆实现
 * <p>
 * 提供基于会话的记忆管理功能，核心特性包括：
 * <ul>
 *     <li><b>懒加载</b>：首次访问时从存储中加载会话历史</li>
 *     <li><b>Token 限制</b>：自动管理记忆大小，不超过最大 Token 数</li>
 *     <li><b>智能压缩</b>：当记忆超出限制时，使用 LLM 生成摘要并压缩历史</li>
 *     <li><b>LRU 缓存</b>：使用 LinkedHashMap 实现最近最少使用策略，自动淘汰旧会话</li>
 * </ul>
 * </p>
 *
 * @see Memory
 */
public class WorkingMemory implements Memory {

    /**
     * 会话映射表：sessionId -> Session
     * 使用 LinkedHashMap 实现 LRU 策略
     */
    private final Map<String, Session> sessionMap;

    /**
     * 记忆存储器
     */
    private final MemoryStore store;
    
    /**
     * DashScope 客户端
     */
    private final DashscopeClient client;
    
    /**
     * 用于生成摘要的聊天模型
     */
    private final ChatModel model;
    
    /**
     * 最大 Token 数
     */
    private final int maxTokens;
    
    /**
     * 保留的 Token 数（用于 GC 后保留的上下文）
     */
    private final int retainTokens;

    /**
     * 构造工作记忆
     *
     * @param builder 构建器
     */
    public WorkingMemory(Builder builder) {

        Objects.requireNonNull(builder.client, "client must not be null");
        Objects.requireNonNull(builder.model, "model must not be null");
        Objects.requireNonNull(builder.store, "store must not be null");
        CheckUtils.require(builder.maxTokens, t -> t > 0, "maxTokens must be greater than 0");
        CheckUtils.require(builder.gcRatio, t -> t > 0 && t < 1, "gcRatio must in (0,1)");
        CheckUtils.require(builder.maxSessions, t -> t > 0, "maxSessions must be greater than 0");

        // 创建支持 LRU 策略的会话映射表
        this.sessionMap = Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Session> eldest) {
                        return size() > builder.maxSessions;
                    }
                }
        );
        this.store = builder.store;
        this.client = builder.client;
        this.model = builder.model;
        this.maxTokens = builder.maxTokens;
        // 计算保留 Token 数：maxTokens * gcRatio
        this.retainTokens = (int) (maxTokens * builder.gcRatio);

    }

    /**
     * 召回会话记忆
     * <p>
     * 从存储中加载会话历史，返回不超过最大 Token 数的消息列表。
     * 采用懒加载策略，首次访问时才会从存储中读取数据。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param instant   当前用户消息（用于上下文参考）
     * @return 召回的消息列表
     */
    @Override
    public CompletionStage<List<Message>> recall(String sessionId, UserMessage instant) {
        return sessionMap.computeIfAbsent(sessionId, k -> new Session())
                .cacheGet(() -> {
                    final var tokensRef = new AtomicInteger();
                    // 从存储中流式读取片段，直到达到最大 Token 数
                    return Flux.from(store.flow(sessionId, Long.MAX_VALUE))
                            .takeWhile(fragment -> tokensRef.addAndGet(fragment.tokens()) <= maxTokens)
                            .collectList()
                            .toFuture();
                });
    }

    /**
     * 记住消息
     * <p>
     * 将消息列表存储到记忆中，并在需要时触发压缩操作。
     * </p>
     *
     * @param sessionId 会话 ID
     * @param messages  要存储的消息列表
     * @return 完成时的 CompletionStage
     */
    @Override
    public CompletionStage<Void> remember(String sessionId, List<Message> messages) {

        final var session = sessionMap.get(sessionId);
        if (null == session) {
            return CompletableFuture.completedStage(null);
        }

        // 存储消息并推送至会话缓存，必要时触发压缩
        return store.upsert(sessionId, messages)
                .thenCompose(fragment -> session.push(maxTokens, fragment, this::compressHistory));
    }

    /**
     * 压缩历史对话
     * <p>
     * 当会话记忆超出 Token 限制时，调用 LLM 对旧的历史对话进行摘要，
     * 生成简洁的摘要消息以替代原始对话内容。
     * </p>
     *
     * @param _fragments 待压缩的片段列表
     * @return 压缩结果（包含保留的片段和生成的摘要）
     */
    private CompletionStage<Session.CompressResult> compressHistory(List<Fragment> _fragments) {
        final var fragments = new ArrayList<>(_fragments);
        // 执行紧凑化，分离出需要压缩的片段
        final var evictions = compact(retainTokens, fragments);

        // 按时间倒序排列被驱逐的片段，拼接为历史对话
        final var history = evictions.stream()
                .sorted((o1, o2) -> Long.compare(o2.fragmentId(), o1.fragmentId()))
                .flatMap(f -> f.messages().stream())
                .toList();

        // 构建摘要生成请求
        final var request = AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .addMessages(history)
                        .addMessage(Message.user("""
                                你是一个专业的对话摘要助手。请总结对话历史，生成一个简洁但全面的摘要。摘要应该：
                                1. 保留关键信息和重要细节
                                2. 忽略寒暄和无关内容
                                3. 用简洁的语言总结主要话题和结论
                                4. 保持在 200-500 字以内
                                5. 只输出摘要内容，不要添加任何解释或额外说明
                                """))
                        .build())
                .build();

        return client.async(request)
                .thenApply(response -> response.output().best().message())
                .thenApply(message -> new Session.CompressResult(fragments, message));
    }

    /**
     * 紧凑化片段列表
     * <p>
     * 从片段列表中移除超出保留 Token 数的旧片段，
     * 返回被移除的片段列表（按插入顺序）。
     * </p>
     *
     * @param retainTokens 保留的 Token 数
     * @param fragments    片段列表（会被修改）
     * @return 被移除的片段列表
     */
    private static List<Fragment> compact(int retainTokens, List<Fragment> fragments) {
        final var evictions = new ArrayList<Fragment>();
        int tokens = 0;
        boolean evictFlag = false;
        final var removeIt = fragments.iterator();
        while (removeIt.hasNext()) {
            final var fragment = removeIt.next();
            // 如果累计 Token 数未超过保留限制，则保留该片段
            if (!evictFlag && !(evictFlag = !(tokens + fragment.tokens() <= retainTokens))) {
                tokens += fragment.tokens();
            } else {
                // 超出限制，移除该片段
                removeIt.remove();
                evictions.add(0, fragment);
            }
        }
        return evictions;
    }

    /**
     * 关闭工作记忆
     * <p>
     * 释放底层存储器资源。
     * </p>
     */
    @Override
    public void close() {
        IOUtils.closeQuietly(store);
    }

    /**
     * 会话缓存
     * <p>
     * 封装了单个会话的记忆缓存逻辑，支持懒加载和自动压缩。
     * </p>
     */
    private static class Session {

        /**
         * 片段列表（最新的在前）
         */
        private final List<Fragment> fragments = new ArrayList<>();
        
        /**
         * 摘要消息引用
         */
        private final AtomicReference<Message> summaryRef = new AtomicReference<>();
        
        /**
         * 是否已加载
         */
        private volatile boolean loaded = false;

        /**
         * 从缓存获取消息列表
         * <p>
         * 如果缓存未加载，则调用 loader 异步加载数据。
         * 加载完成后，将片段拼接为消息列表（先添加摘要，再倒序添加片段）。
         * </p>
         *
         * @param loader 数据加载器
         * @return 消息列表的 CompletionStage
         */
        public CompletionStage<List<Message>> cacheGet(Supplier<CompletionStage<List<Fragment>>> loader) {
            final var getF = new CompletableFuture<List<Fragment>>();

            // 先从缓存中获取，若缓存没有则调用loader进行加载
            synchronized (this) {
                if (!loaded) {
                    Objects.requireNonNull(loader.get())
                            .whenComplete((fragments, ex) -> {
                                if (ex == null) {
                                    synchronized (this) {
                                        if (!loaded) {
                                            loaded = true;
                                            this.fragments.addAll(fragments);
                                        }
                                    }
                                    getF.complete(this.fragments);
                                } else {
                                    getF.completeExceptionally(ex);
                                }
                            });
                } else {
                    getF.complete(this.fragments);
                }
            }

            // 获取到数据后拼接为消息列表
            return getF
                    .thenApply(fragments -> {
                        final var messages = new ArrayList<Message>();

                        // 先添加摘要
                        final var summary = summaryRef.get();
                        if (null != summary) {
                            messages.add(summary);
                        }

                        // 倒序添加片段
                        for (int i = fragments.size() - 1; i >= 0; i--) {
                            messages.addAll(fragments.get(i).messages());
                        }

                        return messages;
                    })
                    .thenApply(Collections::unmodifiableList);
        }

        /**
         * 推送新片段到会话缓存
         * <p>
         * 将新片段添加到缓存头部，如果总 Token 数超过限制，则触发压缩操作。
         * </p>
         *
         * @param maxTokens 最大 Token 数
         * @param fragment  新片段
         * @param compress  压缩函数
         * @return 完成时的 CompletionStage
         */
        public CompletionStage<Void> push(int maxTokens, Fragment fragment, Function<List<Fragment>, CompletionStage<CompressResult>> compress) {
            fragments.add(0, fragment);

            // 计算缓存中的tokens
            final var tokens = fragments.stream()
                    .map(Fragment::tokens)
                    .reduce(Integer::sum)
                    .orElse(0);

            if (tokens <= maxTokens) {
                return CompletableFuture.completedStage(null);
            }

            // 超出限制，执行压缩
            return compress.apply(fragments)
                    .thenAccept(result -> {
                        synchronized (this) {
                            fragments.clear();
                            fragments.addAll(result.compacts());
                            summaryRef.set(result.summary());
                        }
                    });

        }

        /**
         * 压缩结果
         *
         * @param compacts 保留的片段列表
         * @param summary  生成的摘要消息
         */
        public record CompressResult(List<Fragment> compacts, Message summary) {

        }

    }

    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * WorkingMemory 构建器
     * <p>
     * 使用 Builder 模式配置工作记忆。
     * </p>
     */
    public static class Builder implements Buildable<WorkingMemory, Builder> {

        /**
         * 记忆存储器
         */
        private MemoryStore store;
        
        /**
         * DashScope 客户端
         */
        private DashscopeClient client;
        
        /**
         * 聊天模型（用于生成摘要）
         */
        private ChatModel model;
        
        /**
         * 最大 Token 数
         */
        private int maxTokens;
        
        /**
         * GC 比例（0-1 之间），决定压缩后保留的 Token 比例
         */
        private double gcRatio;
        
        /**
         * 最大会话数，默认为 100
         */
        private int maxSessions = 100;

        /**
         * 设置记忆存储器
         *
         * @param store 存储器实例
         * @return 当前构建器
         */
        public Builder store(MemoryStore store) {
            this.store = store;
            return this;
        }

        /**
         * 设置 DashScope 客户端
         *
         * @param client DashScope 客户端
         * @return 当前构建器
         */
        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        /**
         * 设置聊天模型
         *
         * @param model 聊天模型
         * @return 当前构建器
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * 设置最大 Token 数
         *
         * @param maxTokens 最大 Token 数
         * @return 当前构建器
         */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 设置 GC 比例
         *
         * @param gcRatio GC 比例（0-1 之间）
         * @return 当前构建器
         */
        public Builder gcRatio(double gcRatio) {
            this.gcRatio = gcRatio;
            return this;
        }

        /**
         * 设置最大会话数
         *
         * @param maxSessions 最大会话数
         * @return 当前构建器
         */
        public Builder maxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
            return this;
        }

        /**
         * 构建工作记忆
         *
         * @return 新创建的工作记忆实例
         */
        @Override
        public WorkingMemory build() {
            return new WorkingMemory(this);
        }

    }

}
