package io.github.oldmanpushcart.internal.dashscope4j.util;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;

import java.util.concurrent.CompletableFuture;
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
    public static CompletableFuture<String> asyncGetOrPut(Cache cache, String key, Supplier<CompletableFuture<String>> supplier) {
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

}
