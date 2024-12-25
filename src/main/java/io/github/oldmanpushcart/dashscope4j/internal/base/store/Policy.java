package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
class Policy {

    String value;
    String signature;
    Instant expireAt;
    long max;
    long capacity;
    Oss oss;

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Oss {
        String host;
        String directory;
        String ak;
        String acl;
        boolean isForbidOverwrite;
    }

    public boolean isExpired() {
        return expireAt.isBefore(Instant.now());
    }

}
