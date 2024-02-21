package io.github.ompc.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.chat.message.Content;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.dashscope4j.internal.algo.AlgoRequest;
import io.github.ompc.dashscope4j.internal.api.ApiData;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话请求
 */
public class ChatRequest extends AlgoRequest<ChatModel, ChatRequest.Data> {

    protected ChatRequest(Builder builder) {
        super(builder);
    }

    /**
     * 对话请求数据
     *
     * @param messages 对话消息列表
     */
    public record Data(
            @JsonProperty("messages")
            List<Message> messages

    ) implements ApiData {

    }

    /**
     * 对话请求构建器
     */
    public static class Builder extends AlgoRequest.Builder<ChatModel, ChatRequest.Data, ChatRequest, Builder> {

        public Builder() {
            super(new Data(new ArrayList<>()));
        }

        /**
         * 添加消息
         *
         * @param messages 消息
         * @return 构建器
         */
        public Builder messages(Message... messages) {
            return messages(List.of(messages));
        }

        /**
         * @param messages 消息
         * @return 构建器
         * @see #messages(Message...)
         */
        public Builder messages(List<Message> messages) {
            this.input().messages().addAll(messages);
            return this;
        }

        /**
         * 添加系统文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        public Builder system(String text) {
            this.input().messages().add(Message.ofSystem(text));
            return this;
        }

        /**
         * 添加AI文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        public Builder ai(String text) {
            this.input().messages().add(Message.ofAi(text));
            return this;
        }

        /**
         * 添加用户文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        public Builder user(String text) {
            this.input().messages().add(Message.ofUser(text));
            return this;
        }

        /**
         * 添加用户消息
         *
         * @param contents 内容
         * @return 构建器
         */
        public Builder user(Content<?>... contents) {
            this.input().messages().add(Message.ofUser(contents));
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
