package io.github.ompc.internal.dashscope4j.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatPlugin;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.internal.dashscope4j.base.algo.AlgoRequestImpl;
import io.github.ompc.internal.dashscope4j.base.api.http.HttpHeader;
import io.github.ompc.internal.dashscope4j.util.JacksonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;

import static java.util.stream.Collectors.toMap;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final List<ChatPlugin> plugins;

    ChatRequestImpl(ChatModel model, Object input, Option option, Duration timeout, List<ChatPlugin> plugins) {
        super(model, input, option, timeout, ChatResponseImpl.class);
        this.plugins = plugins;
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

    @Override
    public List<ChatPlugin> plugins() {
        return plugins;
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
            final var pluginArgJson = JacksonUtils.toJson(mapper, pluginArgMap);
            builder.header(HttpHeader.HEADER_X_DASHSCOPE_PLUGIN, pluginArgJson);
        }

        return builder.build();
    }

}
