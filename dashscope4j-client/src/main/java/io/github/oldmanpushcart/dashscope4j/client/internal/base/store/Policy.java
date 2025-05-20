package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Duration;
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

    /**
     * @return 是否已过期
     */
    public boolean isExpired() {
        // 这里需要扣掉10秒，防止边界情况
        return expireAt
                .minus(Duration.ofSeconds(10))
                .isBefore(Instant.now());
    }

}
