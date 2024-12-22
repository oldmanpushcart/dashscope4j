package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Cache;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class LruCacheImpl implements Cache {

    private final int capacity;
    private final Map<CompositeKey, Cache.Entry> stringEntryMap = new LinkedHashMap<CompositeKey, Cache.Entry>() {

        @Override
        protected boolean removeEldestEntry(Map.Entry<CompositeKey, Cache.Entry> eldest) {
            return size() > capacity;
        }

    };

    LruCacheImpl(int capacity) {
        this.capacity = capacity;
    }

    @Value
    @Accessors(fluent = true)
    private static class CompositeKey {
        String namespace;
        String key;
    }

    @Override
    public Optional<Entry> get(String namespace, String key) {
        final CompositeKey compositeKey = new CompositeKey(namespace, key);
        synchronized (this) {
            return Optional.ofNullable(stringEntryMap.get(compositeKey));
        }
    }

    @Override
    public Optional<Entry> put(String namespace, String key, byte[] payload) {
        return put(namespace, key, payload, Instant.MAX);
    }

    @Override
    public Optional<Entry> put(String namespace, String key, byte[] payload, Instant expireAt) {
        final CompositeKey compositeKey = new CompositeKey(namespace, key);
        final byte[] copy = Arrays.copyOf(payload, payload.length);
        final Cache.Entry entry = new EntryImpl(namespace, key, expireAt, copy);
        synchronized (this) {
            return Optional.ofNullable(stringEntryMap.put(compositeKey, entry));
        }
    }

    @Override
    public Optional<Entry> remove(String namespace, String key) {
        final CompositeKey compositeKey = new CompositeKey(namespace, key);
        synchronized (this) {
            return Optional.ofNullable(stringEntryMap.remove(compositeKey));
        }
    }

    @Override
    public void close() {

    }

    @Value
    @Accessors(fluent = true)
    static class EntryImpl implements Entry {

        String namespace;
        String key;
        Instant expireAt;
        byte[] payload;

        public byte[] payload() {
            return Arrays.copyOf(payload, payload.length);
        }

        @Override
        public boolean isExpired() {
            return Optional.of(expireAt)
                    .map(Instant.now()::isAfter)
                    .orElse(false);
        }

        @Override
        public boolean isNotExpired() {
            return !isExpired();
        }

    }

}
