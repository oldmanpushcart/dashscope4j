package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function.ChatFunctionToolImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.APPEND_TAIL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

public class ChatRequestBuilderImpl
        extends AlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder>
        implements ChatRequest.Builder {

    private final List<Message> messages = new ArrayList<>();
    private final List<Plugin> plugins = new ArrayList<>();
    private final List<Tool> tools = new ArrayList<>();

    public ChatRequestBuilderImpl() {
    }

    public ChatRequestBuilderImpl(ChatRequest request) {
        super(requireNonNull(request));
        updateList(REPLACE_ALL, this.messages, request.messages());
        updateList(REPLACE_ALL, this.plugins, request.plugins());
        updateList(REPLACE_ALL, this.tools, request.tools());
    }

    @Override
    public ChatRequest.Builder plugins(List<Plugin> plugins) {
        requireNonNull(plugins);
        updateList(REPLACE_ALL, this.plugins, plugins);
        return this;
    }

    @Override
    public ChatRequest.Builder functions(List<ChatFunction<?, ?>> functions) {
        requireNonNull(functions);
        final var tools = functions.stream()
                .map(ChatFunctionToolImpl::byAnnotation)
                .map(Tool.class::cast)
                .toList();
        return tools(tools);
    }

    @Override
    public ChatRequest.Builder tools(List<Tool> tools) {
        requireNonNull(tools);
        updateList(REPLACE_ALL, this.tools, tools);
        return this;
    }

    @Override
    public ChatRequest.Builder messages(List<Message> messages) {
        requireNonNull(messages);
        updateList(REPLACE_ALL, this.messages, messages);
        return this;
    }

    @Override
    public ChatRequest.Builder appendMessages(List<Message> messages) {
        requireNonNull(messages);
        updateList(APPEND_TAIL, this.messages, messages);
        return this;
    }

    @Override
    public ChatRequest build() {
        requireNonNull(model(), "model is required");
        check(messages, CollectionUtils::isNotEmptyCollection, "messages is empty!");
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
