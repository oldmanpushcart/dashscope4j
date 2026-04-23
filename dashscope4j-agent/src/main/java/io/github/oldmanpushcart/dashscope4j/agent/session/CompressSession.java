package io.github.oldmanpushcart.dashscope4j.agent.session;

import io.github.oldmanpushcart.dashscope4j.agent.session.compressor.FragmentCompressor;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FragmentStore;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.FragmentStore.Fragment;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
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
import java.util.function.Function;
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
 * @see CompressSessionManager
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
    private final FragmentStore store;

    /**
     * 片段压缩器
     */
    private final FragmentCompressor compressor;

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
     * 是否已关闭
     */
    private volatile boolean closed = false;

    /**
     * 构造压缩会话
     *
     * @param sessionId    会话 ID
     * @param store        片段存储器
     * @param compressor   片段压缩器
     * @param maxTokens    最大 Token 数（触发压缩的阈值）
     * @param retainTokens 保留的 Token 数（压缩后保留的上下文大小）
     */
    CompressSession(String sessionId, FragmentStore store, FragmentCompressor compressor, int maxTokens, int retainTokens) {
        this.sessionId = sessionId;
        this.store = store;
        this.compressor = compressor;
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
            return Flux.from(store.flow(sessionId, Long.MAX_VALUE))
                    .takeWhile(fragment -> tokensRef.addAndGet(fragment.tokens()) <= maxTokens)
                    .collectList()
                    .toFuture();
        });
    }

    @Override
    public CompletionStage<Void> remember(List<Message> messages) {
        // 将消息持久化到存储，然后推送到内存缓存并检查是否需要压缩
        return store.insert(sessionId, messages)
                .thenCompose(fragment -> push(fragment, fragments -> compressor.compress(fragments, retainTokens)))
                .thenAccept(u -> {

                });
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
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Session is closed"));
        }

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
     * @param fragment   新片段
     * @param compressor 压缩函数，接收片段列表，返回压缩结果
     * @return 完成时的 CompletionStage
     */
    public CompletionStage<Void> push(Fragment fragment, Function<List<Fragment>, CompletionStage<FragmentCompressor.Result>> compressor) {
        if (closed) {
            return CompletableFuture.failedFuture(new IllegalStateException("Session is closed"));
        }

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
        return compressor.apply(fragments)
                .handle((result, ex) -> {

                    // 压缩失败，使用非压缩结果
                    if (ex != null) {
                        logger.warn("{}/compress failed. non-compress used.", this, ex);
                        return null;
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
                        final var rate = String.format("%.2f", after * 100.0f / tokens );
                        logger.debug("{}/compress {} -> {} tokens, rate={}%", this, tokens, after, rate);
                        return null;
                    }

                });

    }

    /**
     * 关闭会话
     */
    @Override
    public void close() {
        closed = true;
    }

}
