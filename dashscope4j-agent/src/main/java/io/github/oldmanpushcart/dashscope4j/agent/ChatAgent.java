package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import org.reactivestreams.Publisher;

import java.util.concurrent.CompletionStage;

public interface ChatAgent extends Agent {

    CompletionStage<AssistantMessage> async(UserMessage message);

    Publisher<AssistantMessage> flow(UserMessage message);

}
