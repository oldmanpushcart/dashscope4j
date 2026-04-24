package io.github.oldmanpushcart.dashscope4j.agent.session2;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface Session {

    CompletionStage<List<Message>> recall(UserMessage inbound);

    CompletionStage<Void> remember(List<Message> messages);

}
