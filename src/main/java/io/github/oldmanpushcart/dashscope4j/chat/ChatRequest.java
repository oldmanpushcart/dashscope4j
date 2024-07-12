package io.github.oldmanpushcart.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.chat.ChatRequestBuilderImpl;

import java.util.List;

/**
 * 对话请求
 */
public interface ChatRequest extends AlgoRequest<ChatModel, ChatResponse> {

    /**
     * @return 对话消息列表
     */
    List<Message> messages();

    /**
     * @return 插件列表
     */
    List<Plugin> plugins();

    /**
     * @return 工具列表
     */
    List<Tool> tools();

    /**
     * @return 对话请求构建器
     */
    static Builder newBuilder() {
        return new ChatRequestBuilderImpl();
    }

    /**
     * 对话请求构建器
     *
     * @param request 对话请求
     * @return 对话请求构建器
     */
    static Builder newBuilder(ChatRequest request) {
        return new ChatRequestBuilderImpl(request);
    }

    /**
     * 对话请求构建器
     */
    interface Builder extends AlgoRequest.Builder<ChatModel, ChatRequest, Builder> {

        /**
         * 设置插件集合
         *
         * @param plugins  插件集合
         * @return this
         */
        Builder plugins(List<Plugin> plugins);

        /**
         * 设置函数集合
         *
         * @param functions 函数集合
         * @return this
         */
        Builder functions(List<ChatFunction<?, ?>> functions);

        /**
         * 设置工具集合
         *
         * @param tools    工具集合
         * @return this
         */
        Builder tools(List<Tool> tools);

        /**
         * 设置消息集合
         *
         * @param messages 消息集合
         * @return this
         */
        Builder messages(List<Message> messages);

        /**
         * 添加消息集合
         * @param messages 消息集合
         * @return this
         */
        Builder appendMessages(List<Message> messages);

    }

}
