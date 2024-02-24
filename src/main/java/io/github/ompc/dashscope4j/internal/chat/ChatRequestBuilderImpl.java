package io.github.ompc.dashscope4j.internal.chat;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.internal.algo.AlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

public class ChatRequestBuilderImpl
        extends AlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder>
        implements ChatRequest.Builder {

    private final List<Message> messages = new ArrayList<>();

    @Override
    public ChatRequest.Builder messages(List<Message> messages) {
        this.messages.addAll(messages);
        return this;
    }

    @Override
    public ChatRequest build() {
        return new ChatRequestImpl(model(), new ChatRequest.Input(messages), option(), timeout());
    }

}
