package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCache;
import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCacheFactory;

import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_FILES;
import static io.github.oldmanpushcart.dashscope4j.Constants.CACHE_NAMESPACE_FOR_UPLOAD;
import static java.lang.Integer.MAX_VALUE;

public class LruCacheFactoryImpl implements PersistentCacheFactory {

    @Override
    public PersistentCache make(String namespace) {
        return switch (namespace) {
            case CACHE_NAMESPACE_FOR_UPLOAD -> new PersistentCacheProxy(new LruCacheImpl<>(namespace, 20480));
            case CACHE_NAMESPACE_FOR_FILES -> new PersistentCacheProxy(new LruCacheImpl<>(namespace, MAX_VALUE));
            default -> throw new UnsupportedOperationException("unsupported namespace: " + namespace);
        };
    }

}
