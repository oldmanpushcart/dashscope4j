package io.github.ompc.dashscope4j.internal.chat;

import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class ChatRequestBuilderImpl implements ChatRequest.Builder {

    private ChatModel model;
    private final List<Message> messages = new ArrayList<>();
    private final Option option = new Option();
    private Duration timeout;

    @Override
    public ChatRequest.Builder messages(List<Message> messages) {
        this.messages.addAll(messages);
        return this;
    }

    @Override
    public ChatRequest.Builder model(ChatModel model) {
        this.model = requireNonNull(model);
        return this;
    }

    @Override
    public <OT, OR> ChatRequest.Builder option(Option.Opt<OT, OR> opt, OT value) {
        this.option.option(opt, value);
        return this;
    }

    @Override
    public ChatRequest.Builder option(String name, Object value) {
        this.option.option(name, value);
        return this;
    }

    @Override
    public ChatRequest.Builder timeout(Duration timeout) {
        this.timeout = requireNonNull(timeout);
        return this;
    }

    @Override
    public ChatRequest build() {
        return new ChatRequestImpl(model, new ChatRequest.Input(messages), option, timeout);
    }

}
