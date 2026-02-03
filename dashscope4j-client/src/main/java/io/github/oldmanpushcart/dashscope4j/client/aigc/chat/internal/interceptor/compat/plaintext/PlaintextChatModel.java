package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.plaintext;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;

import java.util.HashMap;
import java.util.List;

record PlaintextChatModel(
        String name,
        String path
) implements AigcModel<PlaintextChatModel.Input, ChatModel.Output> {

    public record Input(List<Message> messages) {

        @JsonProperty("messages")
        List<Object> plaintextMessages() {
            return messages.stream()
                    .map(message -> {
                        if (message instanceof UserMessage
                                || message instanceof AssistantMessage
                                || message instanceof SystemMessage) {
                            final var pojo = new HashMap<>();
                            pojo.put("role", message.role());
                            pojo.put("content", message.text());
                            return pojo;
                        } else {
                            return message;
                        }
                    })
                    .toList();
        }

    }

}
