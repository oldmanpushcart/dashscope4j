package io.github.oldmanpushcart.dashscope4j.client.base.files;

import java.time.Instant;

public record FileMeta(
        String identity,
        String name,
        long size,
        Instant uploadedAt,
        Purpose purpose
) {
}
