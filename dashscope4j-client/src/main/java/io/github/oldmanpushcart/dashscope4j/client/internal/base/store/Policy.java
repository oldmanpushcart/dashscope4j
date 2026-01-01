package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import java.time.Duration;
import java.time.Instant;

public record Policy(
        String value,
        String signature,
        Instant expireAt,
        long max,
        long capacity,
        Oss oss
) {

    /**
     * @return 是否已过期
     */
    public boolean isExpired() {
        // 这里需要扣掉10秒，防止边界情况
        return expireAt
                .minus(Duration.ofSeconds(10))
                .isBefore(Instant.now());
    }

    public record Oss(
            String host,
            String directory,
            String ak,
            String acl,
            boolean isForbidOverwrite
    ) {

    }

}
