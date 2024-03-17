package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.MessageImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class ChatRequestBuilderImpl extends AlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder> implements ChatRequest.Builder {

    private final List<ChatPlugin> plugins = new ArrayList<>();
    private final List<ChatFunction<?,?>> functions = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public ChatRequest.Builder plugins(ChatPlugin... plugins) {
        this.plugins.addAll(List.of(plugins));
        return this;
    }

    @Override
    public ChatRequest.Builder functions(List<ChatFunction<?,?>> functions) {
        this.functions.addAll(functions);
        return this;
    }

    @Override
    public ChatRequest.Builder messages(List<Message> messages) {
        this.messages.addAll(messages);
        return this;
    }

    @Override
    public ChatRequest build() {

        requireNonNull(model());

        // 为消息设置模型
        messages.stream()
                .filter(message -> message instanceof MessageImpl)
                .map(message -> (MessageImpl) message)
                .forEach(message -> message.model(model()));

        return new ChatRequestImpl(
                model(),
                option(),
                timeout(),
                unmodifiableList(messages),
                unmodifiableList(plugins),
                unmodifiableList(functions)
        );
    }


}
