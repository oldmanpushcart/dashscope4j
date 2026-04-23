package io.github.oldmanpushcart.dashscope4j.agent.session.store;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.TokenizerUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于有序 Map 的片段存储实现
 * <p>
 * 使用 ConcurrentSkipListMap 提供高性能、有序的片段存储和检索能力。
 * </p>
 */
public class HashMapFragmentStore implements FragmentStore {

    /**
     * 内存片段存储（有序、线程安全）
     * key: fragmentId
     * value: Fragment
     */
    private final NavigableMap<Long, Fragment> fragmentMap = new ConcurrentSkipListMap<>();

    /**
     * Fragment ID 生成器
     */
    private final AtomicLong fragmentIdGenerator = new AtomicLong(0);

    @Override
    public Publisher<Fragment> flow(String sessionId, long after) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        // 利用 ConcurrentSkipListMap 的降序视图，避免显式排序
        // descendingMap() 返回的是视图，不会复制数据，性能更优
        final var stream = fragmentMap.descendingMap().values().stream()
                .filter(fragment -> sessionId.equals(fragment.sessionId()))
                .filter(fragment -> fragment.fragmentId() < after);

        return Flux.fromStream(stream);
    }

    @Override
    public CompletionStage<Fragment> insert(String sessionId, List<Message> messages) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(messages, "messages must not be null");

        // 生成 fragment ID
        final long fragmentId = fragmentIdGenerator.incrementAndGet();

        // 估算 tokens
        final int tokens = estimateTokens(messages);

        // 创建片段
        final Fragment fragment = new Fragment(
                fragmentId,
                sessionId,
                messages,
                tokens,
                Instant.now()
        );

        // 存储到 fragmentMap
        fragmentMap.put(fragmentId, fragment);

        return CompletableFuture.completedFuture(fragment);
    }

    @Override
    public CompletionStage<Void> remove(long fragmentId) {
        fragmentMap.remove(fragmentId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        fragmentMap.clear();
    }

    /**
     * 估算消息列表的 tokens 数量
     * <p>
     * 简单实现：按字符数 / 4 估算
     * </p>
     *
     * @param messages 消息列表
     * @return tokens 数量
     */
    private static int estimateTokens(List<Message> messages) {
        return TokenizerUtils.estimateTokens(
                messages.stream()
                        .map(Message::text)
                        .toArray(String[]::new)
        );
    }

}
