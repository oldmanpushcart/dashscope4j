package io.github.oldmanpushcart.dashscope4j.agent.session;

import io.github.oldmanpushcart.dashscope4j.agent.session.store.SessionStore;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.SessionStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 工作会话实现
 * <p>
 * 提供单个会话的记忆管理功能，核心特性包括：
 * <ul>
 *     <li><b>懒加载</b>：首次访问时从存储中加载会话历史</li>
 *     <li><b>Token 限制</b>：自动管理记忆大小，不超过最大 Token 数</li>
 *     <li><b>智能压缩</b>：当记忆超出限制时，使用 LLM 生成摘要并压缩历史</li>
 * </ul>
 * </p>
 *
 * @see Session
 */
class WorkingSession implements Session {

    /**
     * 会话 ID
     */
    private final String sessionId;

    /**
     * 会话存储器
     */
    private final SessionStore store;

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
     * 是否已关闭
     */
    private volatile boolean closed = false;

    /**
     * 构造工作会话
     *
     * @param sessionId    会话 ID
     * @param store        会话存储器
     * @param client       DashScope 客户端
     * @param model        聊天模型
     * @param maxTokens    最大 Token 数
     * @param retainTokens 保留的 Token 数
     */
    WorkingSession(String sessionId, SessionStore store, DashscopeClient client, ChatModel model, int maxTokens, int retainTokens) {
        this.sessionId = sessionId;
        this.store = store;
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
        this.retainTokens = retainTokens;
    }

    @Override
    public CompletionStage<List<Message>> recall(UserMessage instant) {
        return cacheGet(() -> {
            final var tokensRef = new AtomicInteger();
            // 从存储中流式读取片段，直到达到最大 Token 数
            return Flux.from(store.flow(sessionId, Long.MAX_VALUE))
                    .takeWhile(fragment -> tokensRef.addAndGet(fragment.tokens()) <= maxTokens)
                    .collectList()
                    .toFuture();
        });
    }

    @Override
    public CompletionStage<Void> remember(List<Message> messages) {
        return store.upsert(sessionId, messages)
                .thenCompose(fragment -> push(maxTokens, fragment, this::compressHistory));
    }

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
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Session is closed"));
        }

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
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Session is closed"));
        }

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
     * 压缩历史对话
     * <p>
     * 当会话记忆超出 Token 限制时，调用 LLM 对旧的历史对话进行摘要，
     * 生成简洁的摘要消息以替代原始对话内容。
     * </p>
     *
     * @param _fragments 待压缩的片段列表
     * @return 压缩结果（包含保留的片段和生成的摘要）
     */
    private CompletionStage<CompressResult> compressHistory(List<Fragment> _fragments) {
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
                .thenApply(message -> new CompressResult(fragments, message));
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
     * 关闭会话
     */
    @Override
    public void close() {
        closed = true;
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
