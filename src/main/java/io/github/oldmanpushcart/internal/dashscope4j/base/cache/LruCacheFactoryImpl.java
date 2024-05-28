package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;

public class LruCacheFactoryImpl implements CacheFactory {

    @Override
    public <K, V> Cache<K, V> make(String namespace) {
        return switch (namespace) {
            case Constants.CACHE_NAMESPACE_FOR_UPLOAD -> new LruCacheImpl<>(namespace, 20480);
            case Constants.CACHE_NAMESPACE_FOR_FILES -> new LruCacheImpl<>(namespace, Integer.MAX_VALUE);
            default -> throw new UnsupportedOperationException("unsupported namespace: " + namespace);
        };
    }

}
