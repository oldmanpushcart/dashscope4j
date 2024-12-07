package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.util.JacksonUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ChatRequest extends ApiRequest<ChatModel, ChatResponse> {

    private final List<Message> messages;
    private final List<Plugin> plugins;
    private final List<Tool> tools;

    private ChatRequest(Builder builder) {
        super(ChatResponse.class, builder);
        this.messages = builder.messages;
        this.plugins = builder.plugins;
        this.tools = builder.tools;
    }

    @Override
    protected Object input() {
        return new HashMap<Object, Object>() {{
            put("messages", encodeMessages());
        }};
    }

    private ChatModel.Mode switchMode() {

        // 是否有PDFExtract插件
        final boolean hasPdfExtractPlugin = plugins.stream()
                .anyMatch(plugin -> plugin.name().equals(ChatPlugin.PDF_EXTRACTER.name()));

        // 聊天消息列表中是否包含File类型的内容
        final boolean hasFileContent = messages.stream()
                .flatMap(message -> message.contents().stream())
                .anyMatch(content -> content.type() == Content.Type.FILE);

        /*
         * PDFExtract插件比较特殊，
         * 他在有File类型的内容时，消息列表格式为为多模态格式，否则则为文本格式
         */
        if (hasPdfExtractPlugin && hasFileContent) {
            return ChatModel.Mode.MULTIMODAL;
        }

        return model().mode();

    }

    private List<JsonNode> encodeMessages() {
        final ChatModel.Mode mode = switchMode();
        final List<JsonNode> nodes = new LinkedList<>();
        messages.forEach(message -> {
            final JsonNode messageNode = JacksonUtils.toNode(message);
            if (messageNode instanceof ObjectNode) {
                final ObjectNode node = (ObjectNode) messageNode;
                switch (mode) {
                    case TEXT:
                        node.put("content", message.text());
                        break;
                    case MULTIMODAL:
                        node.putPOJO("content", message.contents());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mode: " + mode);
                }
            }
            nodes.add(messageNode);
        });
        return nodes;
    }

    @Override
    public Map<String, String> headers() {
        final Map<String, String> headers = super.headers();

        /*
         * 启用OSS路径解析
         */
        headers.put("X-DashScope-OssResourceResolve", "enable");

        /*
         * 如果有插件，则告知插件列表
         */
        if (!plugins.isEmpty()) {
            final Map<?,?> pluginArgMap = plugins.stream()
                    .collect(toMap(
                            Plugin::name,
                            Plugin::arguments,
                            (a, b) -> b
                    ));
            final String pluginArgJson = JacksonUtils.toJson(pluginArgMap);
            headers.put("X-DashScope-Plugin", pluginArgJson);
        }

        return headers;
    }

    @Override
    public Option option() {
        final Option option = super.option();

        // 插件必选参数
        if (!plugins.isEmpty()) {
            option.option("result_format", "message");
        }

        // 工具必选参数
        if (!tools.isEmpty()) {
            option.option("result_format", "message");
            option.option("tools", tools);
        }

        return option;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<ChatModel, ChatRequest, Builder> {

        private final List<Message> messages;
        private final List<Plugin> plugins;
        private final List<Tool> tools;

        protected Builder() {
            this.messages = new LinkedList<>();
            this.plugins = new LinkedList<>();
            this.tools = new LinkedList<>();
        }

        protected Builder(ChatRequest request) {
            super(request);
            this.messages = new LinkedList<>(request.messages);
            this.plugins = new LinkedList<>(request.plugins);
            this.tools = new LinkedList<>(request.tools);
        }

        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }

        public Builder addMessages(Collection<Message> messages) {
            this.messages.addAll(messages);
            return this;
        }

        public Builder addPlugin(Plugin plugin) {
            this.plugins.add(plugin);
            return this;
        }

        public Builder addPlugins(Collection<Plugin> plugins) {
            this.plugins.addAll(plugins);
            return this;
        }

        public Builder addTool(Tool tool) {
            this.tools.add(tool);
            return this;
        }

        public Builder addTools(Collection<Tool> tools) {
            this.tools.addAll(tools);
            return this;
        }

        public Builder addFunction(ChatFunction<?, ?> function) {
            return addFunctions(Collections.singleton(function));
        }

        public Builder addFunctions(Collection<ChatFunction<?, ?>> functions) {
            final List<Tool> tools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .map(Tool.class::cast)
                    .collect(Collectors.toList());
            this.tools.addAll(tools);
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
