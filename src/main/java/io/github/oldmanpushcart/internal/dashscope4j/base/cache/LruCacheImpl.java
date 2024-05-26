package io.github.oldmanpushcart.internal.dashscope4j.base.cache;

import io.github.oldmanpushcart.dashscope4j.base.cache.Cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class LruCacheImpl<K, V> implements Cache<K, V> {

    private final String namespace;
    private final int capacity;

    private final Map<CacheKey<K>, CacheVal<V>> map = new LinkedHashMap<>() {

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey<K>, CacheVal<V>> eldest) {
            return size() > capacity;
        }

    };

    public LruCacheImpl(String namespace, int capacity) {
        this.namespace = namespace;
        this.capacity = capacity;
    }

    @Override
    public String namespace() {
        return namespace;
    }

    @Override
    public V get(K key) {
        return Optional.ofNullable(map.get(new CacheKey<>(namespace, key)))
                .map(CacheVal::val)
                .orElse(null);
    }

    @Override
    public boolean put(K key, V value) {
        return put(key, value, null);
    }

    @Override
    public boolean put(K key, V value, Duration duration) {
        map.put(
                new CacheKey<>(namespace, key),
                new CacheVal<>(
                        value,
                        Optional.ofNullable(duration)
                                .map(v -> System.currentTimeMillis() + v.toMillis())
                                .orElse(null)
                )
        );
        return true;
    }

    @Override
    public V remove(K key) {
        return Optional.ofNullable(map.remove(new CacheKey<>(namespace, key)))
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
    public Iterator<Entry<K, V>> iterator() {
        final var mapEntryIt = map.entrySet().iterator();
        return new Iterator<>() {

            @Override
            public boolean hasNext() {
                return mapEntryIt.hasNext();
            }

            @Override
            public Entry<K, V> next() {
                final var mapEntry = mapEntryIt.next();
                return new CacheEntry<>(
                        mapEntry.getKey().key(),
                        mapEntry.getValue().val(),
                        Optional.ofNullable(mapEntry.getValue().expireAt())
                                .map(v -> Instant.now().toEpochMilli() > v)
                                .orElse(false)
                );
            }

            @Override
            public void remove() {
                mapEntryIt.remove();
            }

        };
    }

    private record CacheKey<K>(String namespace, K key) {

    }

    private record CacheVal<V>(V val, Long expireAt) {

    }

    private record CacheEntry<K, V>(K key, V value, boolean isExpired) implements Entry<K, V> {

    }


}
