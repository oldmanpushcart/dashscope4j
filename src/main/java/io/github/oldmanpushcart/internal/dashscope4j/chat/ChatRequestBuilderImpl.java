package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function.ChatFunctionToolImpl;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.updateList;
import static java.util.Objects.requireNonNull;

public class ChatRequestBuilderImpl
        extends SpecifyModelAlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder>
        implements ChatRequest.Builder {

    private final List<Message> messages = new ArrayList<>();
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<Tool> tools = new ArrayList<>();

    public ChatRequestBuilderImpl() {
    }

    public ChatRequestBuilderImpl(ChatRequest request) {
        super(request);
        this.messages.addAll(request.messages());
        this.plugins.addAll(request.plugins());
        this.tools.addAll(request.tools());
    }

    @Override
    public ChatRequest.Builder plugins(boolean isAppend, List<Plugin> plugins) {
        updateList(isAppend, this.plugins, plugins);
        return this;
    }

    @Override
    public ChatRequest.Builder functions(boolean isAppend, List<ChatFunction<?, ?>> functions) {
        final var tools = functions.stream()
                .map(ChatFunctionToolImpl::byAnnotation)
                .map(Tool.class::cast)
                .toList();
        return tools(isAppend, tools);
    }

    @Override
    public ChatRequest.Builder tools(boolean isAppend, List<Tool> tools) {
        updateList(isAppend, this.tools, tools);
        return this;
    }

    @Override
    public ChatRequest.Builder messages(boolean isAppend, List<Message> messages) {
        updateList(isAppend, this.messages, messages);
        return this;
    }

    @Override
    public ChatRequest build() {
        requireNonNull(model(), "model is required");
        requireNotEmpty(messages, "messages is required");
        return new ChatRequestImpl(
                model(),
                option(),
                timeout(),
                messages,
                plugins,
                tools
        );
    }


}
