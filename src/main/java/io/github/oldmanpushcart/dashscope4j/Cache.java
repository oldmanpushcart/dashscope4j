package io.github.oldmanpushcart.dashscope4j;

import java.io.Closeable;
import java.time.Instant;
import java.util.Optional;

public interface Cache extends Closeable {

    Optional<Entry> get(String namespace, String key);

    Optional<Entry> put(String namespace, String key, byte[] payload);

    Optional<Entry> put(String namespace, String key, byte[] payload, Instant expireAt);

    Optional<Entry> remove(String namespace, String key);

    interface Entry {

        String namespace();

        String key();

        byte[] payload();

        boolean isExpired();

        Instant expireAt();

    }

}
