package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin.ChatPlugin;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonEmptyCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * 对话请求
 */
@JsonSerialize(using = ChatRequestJsonSerializer.class)
public class ChatRequest extends AlgoRequest<ChatModel, ChatResponse> {

    private final List<Message> messages;
    private final List<Plugin> plugins;
    private final List<Tool> tools;
    private final ChatCompatibility compatibility;

    private ChatRequest(Builder builder) {
        super(ChatResponse.class, builder);
        requireNonEmptyCollection(builder.messages, "messages is empty!");
        this.messages = unmodifiableList(builder.messages);
        this.plugins = unmodifiableList(builder.plugins);
        this.tools = unmodifiableList(builder.tools);
        this.compatibility = builder.compatibility;
    }

    public List<Message> messages() {
        return messages;
    }

    public List<Plugin> plugins() {
        return plugins;
    }

    public List<Tool> tools() {
        return tools;
    }

    public ChatCompatibility compatibility() {
        return compatibility;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

        private final List<Message> messages = new LinkedList<>();
        private final List<Plugin> plugins = new LinkedList<>();
        private final List<Tool> tools = new LinkedList<>();

        private ChatCompatibility compatibility = ChatCompatibility.DASHSCOPE;

        protected Builder() {
        }

        protected Builder(ChatRequest request) {
            super(request);
            this.messages.addAll(request.messages);
            this.plugins.addAll(request.plugins);
            this.tools.addAll(request.tools);
            this.compatibility = request.compatibility;
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
            this.tools.removeIf(tool -> tool instanceof FunctionTool);
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

        public Builder compatibility(ChatCompatibility compatibility) {
            requireNonNull(compatibility);
            this.compatibility = compatibility;
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
