package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class FutureGuard<V> {

    private final AtomicReference<CompletableFuture<V>> futureRef = new AtomicReference<>();

    public CompletableFuture<V> acquire() {
        final var future = new CompletableFuture<V>();
        if (!futureRef.compareAndSet(null, future)) {
            throw new IllegalStateException("FutureGuard is already acquired!");
        }
        return future;
    }

    public boolean completed(V v) {
        final var future = futureRef.get();
        return null != future && future.complete(v);
    }

    public boolean completeExceptionally(Throwable ex) {
        final var future = futureRef.get();
        return null != future && future.completeExceptionally(ex);
    }

    public boolean release(CompletableFuture<V> future) {
        return futureRef.compareAndSet(future, null);
    }

}
