package io.github.oldmanpushcart.dashscope4j.client.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.chat.tool.Tool;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonEmptyCollection;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * 对话请求
 */
public class ChatRequest extends AlgoRequest<ChatModel, ChatResponse> {

    private final List<Message> messages;
    private final List<Tool> tools;
    private final boolean inlineEnabled;
    private final boolean uploadEnabled;

    protected ChatRequest(Builder builder) {
        super(ChatResponse.class, builder);
        requireNonEmptyCollection(builder.messages, "messages is empty!");
        this.messages = unmodifiableList(builder.messages);
        this.tools = unmodifiableList(builder.tools);
        this.inlineEnabled = builder.inlineEnabled;
        this.uploadEnabled = builder.uploadEnabled;
    }

    public List<Message> messages() {
        return messages;
    }

    public List<Tool> tools() {
        return tools;
    }

    @Override
    protected Object input() {
        return new HashMap<>(){{
            put("messages", messages);
        }};
    }

    @JsonProperty("parameters")
    Parameters mergedParameters() {
        final var merged = new Parameters();

        // 强制指定返回格式为"message"，降低返回值的解析复杂度
        merged.append("result_format", "message");

        // 工具必选参数
        if (!tools.isEmpty()) {
            merged.append("tools", tools);
        }

        // 合并原有参数
        merged.merge(super.parameters());

        return merged;
    }

    @JsonIgnore
    @Override
    public Parameters parameters() {
        return super.parameters();
    }

    public boolean inlineEnabled() {
        return inlineEnabled;
    }

    public boolean uploadEnabled() {
        return uploadEnabled;
    }

    public Message lastMessage() {
        return !messages.isEmpty()
                ? messages.get(messages.size() - 1)
                : null;
    }

    public boolean hasUserInputMessage() {
        return !messages.isEmpty()
                && lastMessage().role() == Message.Role.USER;
    }

    public UserMessage userInputMessage() {

        if (!hasUserInputMessage()) {
            return null;
        }

        final var last = lastMessage();
        if (last instanceof UserMessage userMessage) {
            return userMessage;
        } else {
            return null;
        }

    }

    /**
     * 提取历史信息
     * <p>
     * 消息列表中下标范围[0,n-1]信息为历史信息
     * </p>
     *
     * @return 历史信息
     */
    public List<Message> historyMessages() {
        if (messages.isEmpty()) {
            return emptyList();
        }
        if (!hasUserInputMessage()) {
            return messages;
        }
        return messages.subList(0, messages.size() - 1);
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

        private final List<Message> messages = new LinkedList<>();
        private final List<Tool> tools = new LinkedList<>();

        private boolean inlineEnabled = true;
        private boolean uploadEnabled = true;

        protected Builder() {
        }

        protected Builder(ChatRequest request) {
            super(request);
            this.messages.addAll(request.messages);
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

        public Builder inlineEnabled(boolean inlineEnabled) {
            this.inlineEnabled = inlineEnabled;
            return this;
        }

        public Builder uploadEnabled(boolean uploadEnabled) {
            this.uploadEnabled = uploadEnabled;
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
