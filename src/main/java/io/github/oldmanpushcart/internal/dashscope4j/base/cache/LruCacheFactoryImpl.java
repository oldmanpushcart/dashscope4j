package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.CacheFactory;

import static java.lang.Integer.MAX_VALUE;

public class LruCacheFactoryImpl implements CacheFactory {

    @Override
    public Cache make(String namespace) {
        return new LruCacheImpl(namespace, MAX_VALUE);
    }

}
