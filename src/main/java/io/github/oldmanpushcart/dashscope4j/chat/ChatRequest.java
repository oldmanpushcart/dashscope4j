package io.github.oldmanpushcart.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.chat.ChatRequestBuilderImpl;

import java.util.List;

/**
 * 对话请求
 */
public interface ChatRequest extends AlgoRequest<ChatResponse> {

    /**
     * 构建对话请求
     *
     * @return 对话请求构建器
     */
    static Builder newBuilder() {
        return new ChatRequestBuilderImpl();
    }

    /**
     * 对话请求构建器
     */
    interface Builder extends AlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

        /**
         * 添加插件
         *
         * @param plugins 插件
         * @return 构建器
         */
        Builder plugins(ChatPlugin... plugins);

        /**
         * 添加消息
         *
         * @param messages 消息
         * @return 构建器
         */
        default Builder messages(Message... messages) {
            return messages(List.of(messages));
        }

        /**
         * @param messages 消息
         * @return 构建器
         * @see #messages(Message...)
         */
        Builder messages(List<Message> messages);

        /**
         * 添加系统文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        default Builder system(String text) {
            return messages(Message.ofSystem(text));
        }

        /**
         * 添加AI文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        default Builder ai(String text) {
            return messages(Message.ofAi(text));
        }

        /**
         * 添加用户文本消息
         *
         * @param text 文本
         * @return 构建器
         */
        default Builder user(String text) {
            return messages(Message.ofUser(text));
        }

        /**
         * 添加用户消息
         *
         * @param contents 内容
         * @return 构建器
         */
        default Builder user(Content<?>... contents) {
            return messages(Message.ofUser(contents));
        }

    }

}
