package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

public interface Agent {

    String name();

    String description();

    String introduction();

    CompletionStage<AssistantMessage> async(UserMessage inbound);

    Publisher<AssistantMessage> flow(UserMessage inbound);

}
