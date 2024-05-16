package io.github.oldmanpushcart.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.chat.ChatRequestBuilderImpl;

import java.util.List;

/**
 * 对话请求
 */
public interface ChatRequest extends SpecifyModelAlgoRequest<ChatModel, ChatResponse> {

    /**
     * @return 对话消息列表
     * @since 1.4.0
     */
    List<Message> messages();

    /**
     * @return 插件列表
     * @since 1.4.0
     */
    List<Plugin> plugins();

    /**
     * @return 工具列表
     * @since 1.4.0
     */
    List<Tool> tools();

    /**
     * @return 对话请求构建器
     */
    static Builder newBuilder() {
        return new ChatRequestBuilderImpl();
    }

    static Builder newBuilder(ChatRequest request) {
        return new ChatRequestBuilderImpl(request);
    }

    /**
     * 对话请求构建器
     */
    interface Builder extends SpecifyModelAlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

        /**
         * 添加插件
         *
         * @param plugins 插件
         * @return this
         */
        default Builder plugins(ChatPlugin... plugins) {
            return plugins(List.of(plugins).toArray(new Plugin[0]));
        }

        /**
         * 添加插件
         *
         * @param plugins 插件
         * @return this
         * @since 1.4.0
         */
        Builder plugins(Plugin... plugins);

        /**
         * 添加函数
         *
         * @param functions 函数
         * @return this
         * @since 1.2.0
         */
        default Builder functions(ChatFunction<?, ?>... functions) {
            return functions(List.of(functions));
        }

        /**
         * 添加函数
         *
         * @param functions 函数
         * @return this
         * @since 1.2.0
         */
        Builder functions(List<ChatFunction<?, ?>> functions);

        /**
         * 添加工具
         *
         * @param tools 工具
         * @return this
         * @since 1.2.2
         */
        default Builder tools(Tool... tools) {
            return tools(List.of(tools));
        }

        /**
         * 添加工具
         *
         * @param tools 工具
         * @return this
         * @since 1.2.2
         */
        Builder tools(List<Tool> tools);

        /**
         * 添加消息
         *
         * @param messages 消息
         * @return this
         */
        default Builder messages(Message... messages) {
            return messages(List.of(messages));
        }

        /**
         * @param messages 消息
         * @return this
         * @see #messages(Message...)
         */
        Builder messages(List<Message> messages);

        /**
         * 添加系统文本消息
         *
         * @param text 文本
         * @return this
         */
        default Builder system(String text) {
            return messages(Message.ofSystem(text));
        }

        /**
         * 添加AI文本消息
         *
         * @param text 文本
         * @return this
         */
        default Builder ai(String text) {
            return messages(Message.ofAi(text));
        }

        /**
         * 添加用户文本消息
         *
         * @param text 文本
         * @return this
         */
        default Builder user(String text) {
            return messages(Message.ofUser(text));
        }

        /**
         * 添加用户消息
         *
         * @param contents 内容
         * @return this
         */
        default Builder user(Content<?>... contents) {
            return messages(Message.ofUser(contents));
        }

    }

}
