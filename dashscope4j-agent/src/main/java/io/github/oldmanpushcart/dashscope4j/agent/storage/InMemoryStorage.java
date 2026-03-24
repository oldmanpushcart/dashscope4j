package io.github.oldmanpushcart.dashscope4j.agent.storage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用内存存储实现
 *
 * @param <K> 键类型
 * @param <E> 数据元素类型
 */
public class InMemoryStorage<K, E> implements Storage<K, E> {
    
    private final Map<K, E> storage = new ConcurrentHashMap<>();
    private volatile boolean closed = false;
    
    /**
     * 检查是否已关闭，如果已关闭则抛出异常
     */
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Storage is closed");
        }
    }
    
    @Override
    public CompletionStage<Void> init() {
        return CompletableFuture.completedStage(null);
    }
    
    @Override
    public CompletionStage<E> get(K key) {
        try {
            checkNotClosed();
            return CompletableFuture.completedStage(storage.get(key));
        } catch (IllegalStateException ex) {
            return CompletableFuture.failedStage(ex);
        }
    }
    
    @Override
    public CompletionStage<Void> upsert(K key, E item) {
        try {
            checkNotClosed();
            storage.put(key, item);
            return CompletableFuture.completedStage(null);
        } catch (IllegalStateException ex) {
            return CompletableFuture.failedStage(ex);
        }
    }
    
    @Override
    public CompletionStage<Void> remove(K key) {
        try {
            checkNotClosed();
            storage.remove(key);
            return CompletableFuture.completedStage(null);
        } catch (IllegalStateException ex) {
            return CompletableFuture.failedStage(ex);
        }
    }
    
    @Override
    public void close() {
        closed = true;
        storage.clear();
    }
    
}
