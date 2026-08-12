package io.github.oldmanpushcart.dashscope4j.agent.hook.session;

import io.github.oldmanpushcart.dashscope4j.agent.hook.session.storage.FragmentStorage;
import io.github.oldmanpushcart.dashscope4j.agent.hook.session.storage.FragmentStorage.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.TokenizerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 压缩会话实现
 * <p>
 * 提供带智能压缩功能的会话记忆管理，核心特性包括：
 * <ul>
 *     <li><b>懒加载</b>：首次访问时从存储中加载会话历史</li>
 *     <li><b>Token 限制</b>：自动管理记忆大小，不超过最大 Token 数</li>
 *     <li><b>智能压缩</b>：当记忆超出限制时，使用 LLM 生成摘要并压缩历史</li>
 *     <li><b>LRU 缓存友好</b>：与 CompressSessionManager 配合实现最近最少使用策略</li>
 * </ul>
 * </p>
 *
 * @see Session
 */
class CompressSession implements Session {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 会话 ID
     */
    private final String sessionId;

    /**
     * 片段存储器
     */
    private final FragmentStorage storage;

    /**
     * DashScope 客户端（用于 LLM 压缩）
     */
    private final DashscopeClient client;

    /**
     * 聊天模型（用于生成摘要）
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
     * 片段列表（按时间倒序排列，最新的在前）
     */
    private final List<Fragment> fragments = new ArrayList<>();

    /**
     * {@link #toString()} 字符串
     */
    private final String _toString;

    /**
     * 摘要消息的原子引用
     * <p>
     * 用于线程安全地读写压缩后生成的历史摘要消息。
     * </p>
     */
    private final AtomicReference<Message> summaryRef = new AtomicReference<>();

    /**
     * 是否已加载
     */
    private volatile boolean loaded = false;

    /**
     * 构造压缩会话
     *
     * @param sessionId    会话 ID
     * @param storage      片段存储器
     * @param client       DashScope 客户端
     * @param model        聊天模型
     * @param maxTokens    最大 Token 数（触发压缩的阈值）
     * @param retainTokens 保留的 Token 数（压缩后保留的上下文大小）
     */
    public CompressSession(String sessionId, FragmentStorage storage, DashscopeClient client, ChatModel model, int maxTokens, int retainTokens) {
        this.sessionId = sessionId;
        this.storage = storage;
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
        this.retainTokens = retainTokens;
        this._toString = "dashscope4j-agent:/session/%s".formatted(sessionId);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<List<Message>> recall(UserMessage instant) {
        return cacheGet(() -> {
            final var tokensRef = new AtomicInteger();
            // 从存储中流式读取片段，累加 Token 数直到达到最大限制
            return Flux.from(storage.flow(sessionId, Long.MAX_VALUE))
                    .takeWhile(fragment -> tokensRef.addAndGet(fragment.tokens()) <= maxTokens)
                    .collectList()
                    .toFuture();
        });
    }

    @Override
    public CompletionStage<Void> remember(List<Message> messages) {
        // 将消息持久化到存储，然后推送到内存缓存并检查是否需要压缩
        return storage.append(sessionId, messages)
                .thenCompose(this::push);
    }

    /**
     * 从缓存获取消息列表
     * <p>
     * 采用懒加载策略：如果缓存未加载，则调用 loader 异步加载数据。
     * 加载完成后，将片段拼接为完整的消息列表：
     * <ol>
     *     <li>先添加历史摘要消息（如果存在）</li>
     *     <li>再按时间正序添加所有片段中的消息（从旧到新）</li>
     * </ol>
     * </p>
     *
     * @param loader 数据加载器，返回片段列表的 CompletionStage
     * @return 完整消息列表的 CompletionStage（不可变列表）
     */
    public CompletionStage<List<Message>> cacheGet(Supplier<CompletionStage<List<Fragment>>> loader) {

        final var getF = new CompletableFuture<List<Fragment>>();

        // 先从缓存中获取，若缓存未加载则调用 loader 进行异步加载
        synchronized (this) {
            if (!loaded) {
                Objects.requireNonNull(loader.get())
                        .whenComplete((fragments, ex) -> {
                            if (ex == null) {
                                // 双重检查锁定，避免重复加载
                                synchronized (this) {
                                    if (!loaded) {
                                        loaded = true;
                                        this.fragments.addAll(fragments);
                                    }
                                }
                                getF.complete(this.fragments);
                            } else {
                                getF.completeExceptionally(new IllegalStateException(
                                        "session[%s] load fragment failed!".formatted(sessionId),
                                        ex
                                ));
                            }
                        });
            } else {
                getF.complete(this.fragments);
            }
        }

        // 获取到片段数据后，拼接为完整的消息列表
        return getF
                .thenApply(fragments -> {
                    final var messages = new ArrayList<Message>();

                    // 先添加历史摘要（如果存在）
                    final var summary = summaryRef.get();
                    if (null != summary) {
                        messages.add(summary);
                    }

                    // 按时间正序添加片段中的消息（从最旧的片段开始）
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
     * 将新片段添加到缓存头部（最新位置），然后检查总 Token 数：
     * <ul>
     *     <li>如果未超过 maxTokens，直接返回</li>
     *     <li>如果超过 maxTokens，触发压缩操作，生成摘要并清理旧片段</li>
     * </ul>
     * </p>
     *
     * @param fragment 新片段
     * @return 完成时的 CompletionStage
     */
    public CompletionStage<Void> push(Fragment fragment) {

        // 将新片段添加到头部（最新位置）
        fragments.add(0, fragment);

        // 计算当前缓存中的总 Token 数
        final var tokens = fragments.stream()
                .mapToInt(Fragment::tokens)
                .sum();

        // 未超过限制，无需压缩
        if (tokens <= maxTokens) {
            return CompletableFuture.completedStage(null);
        }

        // 超出限制，执行压缩操作
        return compress(fragments)
                .handle((result, ex) -> {

                    // 压缩失败，使用非压缩结果
                    if (ex != null) {
                        logger.warn("{}/compress failed. non-compress used.", this, ex);
                    }

                    // 压缩成功,使用压缩结果
                    else {
                        synchronized (this) {
                            // 清空旧片段,替换为压缩后的结果
                            fragments.clear();
                            fragments.addAll(result.retained());
                            // 更新摘要消息
                            summaryRef.set(result.summary());
                        }

                        // 压缩后的TOKENS
                        final var after
                                = result.retained().stream().mapToInt(Fragment::tokens).sum()
                                + TokenizerUtils.estimateTokens(result.summary().text());

                        // 压缩率
                        final var rate = String.format("%.2f", after * 100.0f / tokens);
                        logger.debug("{}/compress {} -> {} tokens, rate={}%", this, tokens, after, rate);
                    }
                    return null;

                });

    }

    /**
     * 压缩结果记录
     * <p>
     * 封装压缩操作的结果，包含保留的新片段和生成的历史摘要。
     * </p>
     *
     * @param retained 保留的片段列表（较新的片段，未被压缩）
     * @param summary  生成的历史摘要消息
     */
    private record CompressResult(List<Fragment> retained, Message summary) {
    }

    /**
     * 执行片段压缩
     * <p>
     * 当片段总 Token 数超过限制时，使用 LLM 生成历史摘要并压缩旧片段。
     * </p>
     *
     * @param fragments 当前所有片段列表（按时间倒序，最新的在前）
     * @return 压缩结果的 CompletionStage
     */
    private CompletionStage<CompressResult> compress(List<Fragment> fragments) {
        // 创建片段副本，避免修改原始列表
        final var fragmentCopy = new ArrayList<>(fragments);

        // 执行紧凑化，分离出需要压缩的旧片段
        final var evictions = compact(retainTokens, fragmentCopy);

        // 按时间正序排列被驱逐的片段（从旧到新），拼接为历史对话
        final var history = evictions.stream()
                .sorted((o1, o2) -> Long.compare(o2.fragmentId(), o1.fragmentId()))
                .flatMap(f -> f.messages().stream())
                .toList();

        // 构建摘要生成请求
        final var request = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
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
                .thenApply(message -> new CompressResult(fragmentCopy, message));
    }

    /**
     * 紧凑化片段列表
     * <p>
     * 从片段列表中移除超出保留 Token 数的旧片段，保留最近的片段。
     * 片段列表按时间倒序排列（最新的在前），因此从尾部开始移除旧片段。
     * </p>
     *
     * @param retainTokens 保留的 Token 数
     * @param fragments    片段列表（会被修改，移除旧片段）
     * @return 被移除的片段列表（按时间正序排列，从旧到新）
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
                // 超出限制，移除该片段并记录到驱逐列表
                removeIt.remove();
                evictions.add(0, fragment);  // 插入到头部，保持从旧到新的顺序
            }
        }
        return evictions;
    }

}
