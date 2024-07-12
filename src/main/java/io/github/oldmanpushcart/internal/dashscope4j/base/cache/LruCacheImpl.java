package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * LRU缓存实现
 */
public class LruCacheImpl implements Cache {

    private final String namespace;
    private final int capacity;
    private final Map<CacheKey, CacheVal> map = new LinkedHashMap<>() {

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheVal> eldest) {
            return size() > capacity;
        }

    };

    /**
     * 构造LRU缓存
     *
     * @param namespace 命名空间
     * @param capacity  容量
     */
    public LruCacheImpl(String namespace, int capacity) {
        this.namespace = namespace;
        this.capacity = capacity;
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public String get(String key) {
        final var cacheKey = new CacheKey(namespace, key);
        return Optional.ofNullable(map.get(cacheKey))
                .map(CacheVal::val)
                .orElse(null);
    }

    @Override
    public boolean put(String key, String value) {
        return put(key, value, null);
    }

    @Override
    public boolean put(String key, String value, Duration duration) {
        final var cacheKey = new CacheKey(namespace, key);
        final var cacheVal = new CacheVal(value, computeExpireAt(duration));
        map.put(cacheKey, cacheVal);
        return true;
    }

    private static Long computeExpireAt(Duration duration) {
        return Optional.ofNullable(duration)
                .map(v -> System.currentTimeMillis() + v.toMillis())
                .orElse(null);
    }

    @Override
    public String remove(String key) {
        final var cacheKey = new CacheKey(namespace, key);
        return Optional.ofNullable(map.remove(cacheKey))
                .map(CacheVal::val)
                .orElse(null);
    }

    @Override
    public int clear() {
        final var size = map.size();
        map.clear();
        return size;
    }

    @Override
    public Iterator<Entry> iterator() {
        final var mapEntryIt = map.entrySet().iterator();
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return mapEntryIt.hasNext();
            }

            @Override
            public Entry next() {
                return CacheEntry.of(mapEntryIt.next());
            }

            @Override
            public void remove() {
                mapEntryIt.remove();
            }

        };
    }

    /**
     * 缓存键
     *
     * @param namespace 命名空间
     * @param key       键
     */
    private record CacheKey(String namespace, String key) {

    }

    /**
     * 缓存值
     *
     * @param val      值
     * @param expireAt 过期时间
     */
    private record CacheVal(String val, Long expireAt) {

    }

    /**
     * 缓存数据项
     *
     * @param key      键
     * @param value    值
     * @param expireAt 过期时间戳
     */
    private record CacheEntry(String key, String value, Long expireAt) implements Entry {

        @Override
        public boolean isExpired() {
            return Optional.ofNullable(expireAt)
                    .map(v -> System.currentTimeMillis() > v)
                    .orElse(false);
        }

        /**
         * 从Map.Entry转换
         *
         * @param mapEntry Map.Entry
         * @return CacheEntry
         */
        static CacheEntry of(Map.Entry<CacheKey, CacheVal> mapEntry) {
            return new CacheEntry(
                    mapEntry.getKey().key(),
                    mapEntry.getValue().val(),
                    mapEntry.getValue().expireAt()
            );
        }

    }


}
