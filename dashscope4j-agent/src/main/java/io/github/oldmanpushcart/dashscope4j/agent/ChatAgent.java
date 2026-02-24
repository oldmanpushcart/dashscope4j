package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface ChatAgent {

    CompletionStage<AssistantMessage> async(UserMessage message);

    Flow.Publisher<AssistantMessage> flow(UserMessage message);

}
