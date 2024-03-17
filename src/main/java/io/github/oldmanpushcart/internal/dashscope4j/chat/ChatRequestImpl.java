package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    private final ChatModel model;
    private final List<Message> messages;
    private final List<ChatPlugin> plugins;

    ChatRequestImpl(ChatModel model, Option option, Duration timeout, List<Message> messages, List<ChatPlugin> plugins) {
        super(model, null, option, timeout, ChatResponseImpl.class);
        this.model = model;
        this.messages = messages;
        this.plugins = plugins;
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

    @Override
    public Object input() {
        return new HashMap<>() {{
            put("messages", switch (model.mode()) {
                case MULTIMODAL -> unmodifiableList(messages);
                case TEXT -> messages.stream()
                        .map(message -> {
                            final var item = new HashMap<>();
                            item.put("role", message.role());
                            item.put("content", message.text());
                            return item;
                        })
                        .toList();
            });
        }};
    }

    @Override
    public Option option() {
        final var clone = new Option();
        super.option().export().forEach(clone::option);

        // 如果启用了插件服务，必须使用 message 格式
        if(!plugins.isEmpty()) {
            clone.option("result_format", "message");
        }

        return clone;
    }

    @Override
    public HttpRequest newHttpRequest() {
        final var builder = HttpRequest.newBuilder(super.newHttpRequest(), (k, v) -> true);

        // 添加插件
        if (!plugins.isEmpty()) {
            final var pluginArgMap = plugins.stream()
                    .collect(toMap(
                            ChatPlugin::name,
                            ChatPlugin::arguments,
                            (a, b) -> b
                    ));
            final var pluginArgJson = JacksonUtils.toJson(pluginArgMap);
            builder.header(HttpHeader.HEADER_X_DASHSCOPE_PLUGIN, pluginArgJson);
        }

        return builder.build();
    }

}
