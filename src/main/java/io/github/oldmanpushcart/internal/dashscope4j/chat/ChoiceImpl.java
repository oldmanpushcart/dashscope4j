package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ChoiceImpl(ChatResponse.Finish finish, List<Message> history) implements ChatResponse.Choice {

    public ChoiceImpl(ChatResponse.Finish finish, Message message) {
        this(finish, new ArrayList<>(List.of(message)));
    }

    @Override
    public Message message() {
        return history.get(history.size() - 1);
    }

    @Override
    public List<Message> history() {
        return Collections.unmodifiableList(history);
    }

    void appendFirst(List<Message> messages) {
        history.addAll(0, messages);
    }

}
