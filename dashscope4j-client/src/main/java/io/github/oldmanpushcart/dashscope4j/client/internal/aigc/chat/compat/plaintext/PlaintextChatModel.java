package io.github.oldmanpushcart.dashscope4j.client.internal.aigc.chat.compat.plaintext;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.Model;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.HashMap;
import java.util.List;

public record PlaintextChatModel(
        String name,
        String path
) implements Model<PlaintextChatModel.Input, ChatModel.Output> {

    public record Input(List<Message> messages) {

        @JsonProperty("messages")
        List<Object> plaintextMessages() {
            return messages.stream()
                    .map(message -> {
                        if (message instanceof UserMessage
                                || message instanceof AssistantMessage
                                || message instanceof SystemMessage) {
                            return new HashMap<>() {{
                                put("role", message.role());
                                put("content", message.text());
                            }};
                        } else {
                            return message;
                        }
                    })
                    .toList();
        }

    }

}
