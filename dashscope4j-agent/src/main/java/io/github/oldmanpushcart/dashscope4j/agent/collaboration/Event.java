package io.github.oldmanpushcart.dashscope4j.agent.collaboration;

import java.time.Instant;
import java.util.Map;

public record Event(
        String id,
        String topic,
        Map<String, Object> payload,
        Instant createdAt
) {
}
