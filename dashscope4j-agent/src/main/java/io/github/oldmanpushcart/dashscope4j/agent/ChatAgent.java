package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ChatAgent {

    CompletionStage<Message> async(List<Message> messages);

    Flow.Publisher<Message> flow(List<Message> messages);

}
