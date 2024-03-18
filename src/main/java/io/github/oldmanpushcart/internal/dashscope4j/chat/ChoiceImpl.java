package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.List;

public record ChoiceImpl(ChatResponse.Finish finish, List<Message> messages) implements ChatResponse.Choice {

    public ChoiceImpl(ChatResponse.Finish finish, Message message) {
        this(finish, List.of(message));
    }

    @Override
    public Message message() {
        return messages.get(messages.size() - 1);
    }

}
