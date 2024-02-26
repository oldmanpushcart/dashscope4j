package io.github.ompc.internal.dashscope4j.chat;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatPlugin;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class ChatRequestBuilderImpl extends AlgoRequestBuilderImpl<ChatModel, ChatRequest, ChatRequest.Builder> implements ChatRequest.Builder {

    private final List<ChatPlugin> plugins = new ArrayList<>();
    private final List<Message> messages = new ArrayList<>();

    @Override
    public ChatRequest.Builder plugins(ChatPlugin... plugins) {
        this.plugins.addAll(List.of(plugins));
        return this;
    }

    @Override
    public ChatRequest.Builder messages(List<Message> messages) {
        this.messages.addAll(messages);
        return this;
    }

    @Override
    public ChatRequest build() {
        return new ChatRequestImpl(
                requireNonNull(model()),
                makeInput(model(), messages),
                option(),
                timeout(),
                plugins
        );
    }

    private static Object makeInput(ChatModel model, List<Message> messages) {
        return new HashMap<>() {{
            put("messages", switch (model.mode()) {
                case MULTIMODAL -> messages;
                case TEXT -> messages.stream()
                        .map(message -> {
                            final var item = new HashMap<>();
                            item.put("role", message.role());
                            item.put("content", message.text());
                            return item;
                        })
                        .collect(toList());
            });
        }};
    }

}
