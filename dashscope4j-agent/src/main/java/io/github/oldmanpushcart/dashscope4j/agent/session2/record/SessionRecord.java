package io.github.oldmanpushcart.dashscope4j.agent.session2.record;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

import java.time.Instant;
import java.util.List;

public interface SessionRecord {

    long id();

    String sessionId();

    List<Message> messages();

    int tokens();

    Instant createdAt();

}
