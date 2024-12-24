package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import okhttp3.Request;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.*;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;

/**
 * 对话请求
 * <pre><code>
 * {
 *   "model": "qwen-turbo",
 *   "input": {
 *     "messages": [
 *       {
 *         "role": "user",
 *         "content": "hello!"
 *       }
 *     ]
 *   },
 *   "parameters": {}
 * }
 * </code></pre>
 */
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ChatRequest extends AlgoRequest<ChatModel, ChatResponse> {

    private final List<Message> messages;
    private final List<Plugin> plugins;
    private final List<Tool> tools;

    private ChatRequest(Builder builder) {
        super(ChatResponse.class, builder);
        this.messages = unmodifiableList(builder.messages);
        this.plugins = unmodifiableList(builder.plugins);
        this.tools = unmodifiableList(builder.tools);
    }

    @Override
    protected Object input() {
        return new HashMap<Object, Object>() {{
            put("messages", encodeMessages());
        }};
    }

    /**
     * @return 切换对话模型模式
     */
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

        // 否则返回模型的默认模式
        return model().mode();

    }

    /**
     * 根据模式编码消息列表
     * <p>
     * 对话模型模式有文本和多模态两种，不同模态对messages有不同的要求且无法兼容。
     * 更有些Plugin会根据传的内容类型来决定是否启用哪一种模式，所以这里需要根据messages的内容来切换模式。
     * </p>
     *
     * @return 编码后的消息列表
     */
    private List<JsonNode> encodeMessages() {
        final ChatModel.Mode mode = switchMode();
        final List<JsonNode> nodes = new LinkedList<>();
        messages.forEach(message -> {
            final JsonNode messageNode = JacksonJsonUtils.toNode(message);
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
    public Request newHttpRequest() {
        final Request.Builder builder = new Request.Builder(super.newHttpRequest());

        /*
         * 启用OSS路径解析
         */
        builder.addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE);

        /*
         * 如果有插件，则告知插件列表
         */
        if (!plugins.isEmpty()) {
            final Map<?, ?> pluginArgMap = plugins.stream()
                    .collect(toMap(
                            Plugin::name,
                            Plugin::meta,
                            (a, b) -> b
                    ));
            builder.addHeader(HTTP_HEADER_X_DASHSCOPE_PLUGIN, JacksonJsonUtils.toJson(pluginArgMap));
        }

        return builder.build();
    }

    /**
     * @return 请求选项
     * <p>
     * 一些对话场景强制要求设置一些选项，比如工具列表等。
     * </p>
     */
    @Override
    public Option option() {
        final Option option = new Option();

        // 插件必选参数
        if (!plugins.isEmpty()) {
            option.option("result_format", "message");
        }

        // 工具必选参数
        if (!tools.isEmpty()) {
            option.option("result_format", "message");
            option.option("tools", tools);
        }

        return new Option()
                .merge(super.option())
                .merge(option)
                .unmodifiable();
    }

    /**
     * @return 新建对话请求构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 克隆一个对话请求并基于此新建一个对话请求构建器
     *
     * @param request 原始对话请求
     * @return 对话请求构造器
     */
    public static Builder newBuilder(ChatRequest request) {
        return new Builder(request);
    }

    /**
     * 对话请求构建器
     */
    public static class Builder extends AlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

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

        /**
         * 设置消息列表
         *
         * @param messages 消息列表
         * @return this
         */
        public Builder messages(Collection<Message> messages) {
            this.messages.clear();
            this.messages.addAll(messages);
            return this;
        }

        /**
         * 添加消息
         *
         * @param message 消息
         * @return this
         */
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }

        /**
         * 添加消息列表
         *
         * @param messages 消息列表
         * @return this
         */
        public Builder addMessages(Collection<Message> messages) {
            this.messages.addAll(messages);
            return this;
        }

        /**
         * 设置插件列表
         *
         * @param plugins 插件列表
         * @return this
         */
        public Builder plugins(Collection<Plugin> plugins) {
            this.plugins.clear();
            this.plugins.addAll(plugins);
            return this;
        }

        /**
         * 添加插件
         *
         * @param plugin 插件
         * @return this
         */
        public Builder addPlugin(Plugin plugin) {
            this.plugins.add(plugin);
            return this;
        }

        /**
         * 添加插件列表
         *
         * @param plugins 插件列表
         * @return this
         */
        public Builder addPlugins(Collection<Plugin> plugins) {
            this.plugins.addAll(plugins);
            return this;
        }

        /**
         * 设置工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder tools(Collection<Tool> tools) {
            this.tools.clear();
            this.tools.addAll(tools);
            return this;
        }

        /**
         * 添加工具
         *
         * @param tool 工具
         * @return this
         */
        public Builder addTool(Tool tool) {
            this.tools.add(tool);
            return this;
        }

        /**
         * 添加工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder addTools(Collection<Tool> tools) {
            this.tools.addAll(tools);
            return this;
        }

        /**
         * 设置函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public Builder functions(Collection<ChatFunction<?, ?>> functions) {
            this.tools.removeIf(tool -> tool instanceof ChatFunctionTool);
            this.tools.addAll(toTools(functions));
            return this;
        }

        private static List<Tool> toTools(Collection<ChatFunction<?, ?>> functions) {
            return functions.stream()
                    .map(ChatFunctionTool::of)
                    .map(Tool.class::cast)
                    .collect(Collectors.toList());
        }

        /**
         * 添加函数
         *
         * @param function 函数
         * @return this
         */
        public Builder addFunction(ChatFunction<?, ?> function) {
            return addFunctions(Collections.singleton(function));
        }

        /**
         * 添加函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public Builder addFunctions(Collection<ChatFunction<?, ?>> functions) {
            this.tools.addAll(toTools(functions));
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
