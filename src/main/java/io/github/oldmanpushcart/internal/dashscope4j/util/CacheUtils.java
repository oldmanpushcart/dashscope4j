package io.github.oldmanpushcart.internal.dashscope4j.util;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CacheUtils {

    public static <K, V> CompletableFuture<V> asyncGetOrPut(Cache<K, V> cache, K key, Supplier<CompletableFuture<V>> supplier) {
        final var existed = cache.get(key);
        if (null != existed) {
            return CompletableFuture.completedFuture(existed);
        }
        return supplier.get()
                .thenApply(value -> {
                    cache.put(key, value);
                    return value;
                });
    }

    public static <V> V removeIf(Cache<?, V> cache, Predicate<V> filter) {
        final var entryIt = cache.iterator();
        while (entryIt.hasNext()) {
            final var entry = entryIt.next();
            if (filter.test(entry.value())) {
                final var value = entry.value();
                entryIt.remove();
                return value;
            }
        }
        return null;
    }

}
