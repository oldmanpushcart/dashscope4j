package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FutureGuard {

    private final ConcurrentHashMap<Class<?>, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();

    public CompletableFuture<?> acquire(Class<?> type) {
        return null;
    }

    public boolean release(Class<?> type, CompletableFuture<?> future) {
        if (futureMap.remove(type, future)) {
            future.complete(null);
        }
        return false;
    }

}
