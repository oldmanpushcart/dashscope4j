package io.github.ompc.internal.dashscope4j.chat;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.internal.dashscope4j.algo.AlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

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
        return new ChatRequestImpl(
                requireNonNull(model()),
                new ChatRequestImpl.InputImpl(messages),
                option(),
                timeout()
        );
    }

}
