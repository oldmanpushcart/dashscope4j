package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;

import java.util.List;

public class ChoiceImpl implements ChatResponse.Choice {

    private final ChatResponse.Finish finish;
    private final List<Message> messages;

    public ChoiceImpl(ChatResponse.Finish finish, List<Message> messages) {
        this.finish = finish;
        this.messages = messages;
    }

    public ChoiceImpl(ChatResponse.Finish finish, Message message) {
        this(finish, List.of(message));
    }

    @Override
    public ChatResponse.Finish finish() {
        return finish;
    }

    @Override
    public Message message() {
        return messages.get(messages.size() - 1);
    }

    public List<Message> messages() {
        return messages;
    }

}
