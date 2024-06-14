package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;
import io.github.oldmanpushcart.dashscope4j.base.cache.PersistentCache;

import java.time.Duration;
import java.util.Iterator;

/**
 * 持久化缓存代理
 * <p>用于兼容</p>
 */
public class PersistentCacheProxy implements PersistentCache {

    private final Cache<String, String> target;

    public PersistentCacheProxy(Cache<String, String> target) {
        this.target = target;
    }

    @Override
    public String namespace() {
        return target.namespace();
    }

    @Override
    public String get(String key) {
        return target.get(key);
    }

    @Override
    public boolean put(String key, String value) {
        return target.put(key, value);
    }

    @Override
    public boolean put(String key, String value, Duration duration) {
        return target.put(key, value, duration);
    }

    @Override
    public String remove(String key) {
        return target.remove(key);
    }

    @Override
    public int clear() {
        return target.clear();
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        return target.iterator();
    }

}
