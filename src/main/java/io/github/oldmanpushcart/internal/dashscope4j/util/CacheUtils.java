package io.github.oldmanpushcart.internal.dashscope4j.util;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * 缓存工具类
 */
public class CacheUtils {

    /**
     * 异步获取或放入
     *
     * @param cache    缓存
     * @param key      键
     * @param supplier 供应器
     * @return 异步值
     */
    public static CompletionStage<String> asyncGetOrPut(Cache cache, String key, Supplier<CompletionStage<ExpiringValue>> supplier) {
        final var existed = cache.get(key);
        if (null != existed) {
            return CompletableFuture.completedFuture(existed);
        }
        return supplier.get()
                .thenApply(expiringValue -> {
                    cache.put(key, expiringValue.value, expiringValue.duration);
                    return expiringValue.value;
                });
    }

    public record ExpiringValue(String value, Duration duration) {

        public ExpiringValue(String value) {
            this(value, null);
        }

    }

}
