package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function.ChatFunctionToolImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class ChatRequestBuilderImpl extends AlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder> implements ChatRequest.Builder {

    private final List<ChatPlugin> plugins = new ArrayList<>();
    private final List<Tool> tools = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public ChatRequest.Builder plugins(ChatPlugin... plugins) {
        this.plugins.addAll(List.of(plugins));
        return this;
    }

    @Override
    public ChatRequest.Builder functions(List<ChatFunction<?, ?>> functions) {
        final var tools = functions.stream()
                .map(ChatFunctionToolImpl::byAnnotation)
                .toList();
        this.tools.addAll(tools);
        return this;
    }

    @Override
    public ChatRequest.Builder tools(List<Tool> tools) {
        this.tools.addAll(tools);
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
        return new ChatRequestImpl(
                model(),
                option(),
                timeout(),
                unmodifiableList(messages),
                unmodifiableList(plugins),
                unmodifiableList(tools)
        );
    }


}
