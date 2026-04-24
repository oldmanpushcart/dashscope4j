package io.github.oldmanpushcart.dashscope4j.agent.session2.record;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface SessionRecordSource extends AutoCloseable {

    Publisher<SessionRecord> flow(String sessionId, long after);

    CompletionStage<SessionRecord> append(String sessionId, List<Message> messages);

    void subscribe(SessionRecordListener listener);

}
