package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import okhttp3.Request;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.HTTP_HEADER_X_DASHSCOPE_PLUGIN;
import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonEmptyCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
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
        requireNonEmptyCollection(builder.messages, "messages is empty!");
        this.messages = unmodifiableList(builder.messages);
        this.plugins = unmodifiableList(builder.plugins);
        this.tools = unmodifiableList(builder.tools);
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
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
        return messages.stream()
                .map(message -> MessageCodec.encodeToJsonNode(mode, message))
                .collect(Collectors.toList());
    }

    @Override
    public Request newHttpRequest() {
        final Request.Builder builder = new Request.Builder(super.newHttpRequest());

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
        final List<Tool> enabledTools = tools.stream()
                .filter(Tool::isEnabled)
                .collect(Collectors.toList());
        if (!enabledTools.isEmpty()) {
            option.option("result_format", "message");
            option.option("tools", enabledTools);
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

        private final List<Message> messages = new LinkedList<>();
        private final List<Plugin> plugins = new LinkedList<>();
        private final List<Tool> tools = new LinkedList<>();

        protected Builder() {
        }

        protected Builder(ChatRequest request) {
            super(request);
            this.messages.addAll(request.messages);
            this.plugins.addAll(request.plugins);
            this.tools.addAll(request.tools);
        }

        /**
         * 设置消息列表
         *
         * @param messages 消息列表
         * @return this
         */
        public Builder messages(Collection<? extends Message> messages) {
            requireNonNull(messages);
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
            requireNonNull(message);
            this.messages.add(message);
            return this;
        }

        /**
         * 添加消息列表
         *
         * @param messages 消息列表
         * @return this
         */
        public Builder addMessages(Collection<? extends Message> messages) {
            requireNonNull(messages);
            this.messages.addAll(messages);
            return this;
        }

        /**
         * 设置插件列表
         *
         * @param plugins 插件列表
         * @return this
         */
        public Builder plugins(Collection<? extends Plugin> plugins) {
            requireNonNull(plugins);
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
            requireNonNull(plugin);
            this.plugins.add(plugin);
            return this;
        }

        /**
         * 添加插件列表
         *
         * @param plugins 插件列表
         * @return this
         */
        public Builder addPlugins(Collection<? extends Plugin> plugins) {
            requireNonNull(plugins);
            this.plugins.addAll(plugins);
            return this;
        }

        /**
         * 设置工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder tools(Collection<? extends Tool> tools) {
            requireNonNull(tools);
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
            requireNonNull(tool);
            this.tools.add(tool);
            return this;
        }

        /**
         * 添加工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder addTools(Collection<? extends Tool> tools) {
            requireNonNull(tools);
            this.tools.addAll(tools);
            return this;
        }

        /**
         * 设置函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public Builder functions(Collection<? extends ChatFunction<?, ?>> functions) {
            requireNonNull(functions);
            this.tools.removeIf(tool -> tool instanceof ChatFunctionTool);
            this.tools.addAll(toTools(functions));
            return this;
        }

        private static List<Tool> toTools(Collection<? extends ChatFunction<?, ?>> functions) {
            requireNonNull(functions);
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
            requireNonNull(function);
            return addFunctions(Collections.singleton(function));
        }

        /**
         * 添加函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public Builder addFunctions(Collection<? extends ChatFunction<?, ?>> functions) {
            requireNonNull(functions);
            this.tools.addAll(toTools(functions));
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
