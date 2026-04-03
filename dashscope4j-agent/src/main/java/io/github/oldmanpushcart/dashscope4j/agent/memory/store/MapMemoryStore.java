package io.github.oldmanpushcart.dashscope4j.agent.memory.store;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.base.tokenizer.Tokenizer;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.tokenizer.local.LocalTokenizer;
import io.github.oldmanpushcart.dashscope4j.client.util.TokenizerUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于有序 Map 的内存存储实现
 * <p>
 * 使用 ConcurrentSkipListMap 提供高性能、有序的内存片段存储和检索能力。
 * </p>
 */
public class MapMemoryStore implements MemoryStore {

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
    public Publisher<Fragment> flow(String sessionId, long offset) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        
        // 过滤出指定会话的片段，从 offset 开始按序遍历
        return Flux.fromStream(
                fragmentMap.tailMap(offset)
                        .values()
                        .stream()
                        .filter(fragment -> sessionId.equals(fragment.sessionId()))
        );
    }

    @Override
    public CompletionStage<Long> upsert(String sessionId, Message inbound, Message outbound) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        // 生成 fragment ID
        final long fragmentId = fragmentIdGenerator.incrementAndGet();

        // 估算 tokens（简单按字符数 / 4 估算）
        final int tokens = estimateTokens(inbound, outbound);

        // 创建片段
        final Fragment fragment = new Fragment(
                fragmentId,
                sessionId,
                inbound,
                outbound,
                tokens,
                Instant.now()
        );

        // 存储到 fragmentMap
        fragmentMap.put(fragmentId, fragment);

        return CompletableFuture.completedFuture(fragmentId);
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
     * 估算消息的 tokens 数量
     * <p>
     * 简单实现：按字符数 / 4 估算
     * </p>
     *
     * @param inbound  用户输入
     * @param outbound 助手输出
     * @return tokens 数量
     */
    private static int estimateTokens(Message inbound, Message outbound) {
        return TokenizerUtils.estimateTokens(
                inbound.text(),
                outbound.text()
        );
    }

}
