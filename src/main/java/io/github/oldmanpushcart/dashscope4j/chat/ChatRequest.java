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
        default Builder plugins(Plugin... plugins) {
            return plugins(List.of(plugins));
        }

        /**
         * 添加插件集合
         *
         * @param plugins 插件集合
         * @return this
         * @since 1.4.0
         */
        default Builder plugins(List<Plugin> plugins) {
            return plugins(true, plugins);
        }

        /**
         * 添加或设置插件集合
         *
         * @param isAppend 是否添加
         * @param plugins  插件集合
         * @return this
         * @since 1.4.0
         */
        Builder plugins(boolean isAppend, List<Plugin> plugins);

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
        default Builder functions(List<ChatFunction<?, ?>> functions) {
            return functions(true, functions);
        }

        /**
         * 添加或设置函数集合
         *
         * @param isAppend  是否追加
         * @param functions 函数集合
         * @return this
         * @since 1.4.0
         */
        Builder functions(boolean isAppend, List<ChatFunction<?, ?>> functions);

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
         * 添加工具集合
         *
         * @param tools 工具集合
         * @return this
         * @since 1.2.2
         */
        default Builder tools(List<Tool> tools) {
            return tools(true, tools);
        }

        /**
         * 添加或设置工具集合
         *
         * @param isAppend 是否追加
         * @param tools    工具集合
         * @return this
         * @since 1.4.0
         */
        Builder tools(boolean isAppend, List<Tool> tools);

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
         * 添加消息集合
         *
         * @param messages 消息集合
         * @return this
         * @see #messages(Message...)
         */
        default Builder messages(List<Message> messages) {
            return messages(true, messages);
        }

        /**
         * 添加或设置消息集合
         *
         * @param isAppend 是否追加
         * @param messages 消息集合
         * @return this
         * @since 1.4.0
         */
        Builder messages(boolean isAppend, List<Message> messages);

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
