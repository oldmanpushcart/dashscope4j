package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.OpFlow;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

/**
 * 智能体
 */
public interface ChatAgent extends OpAsync<ChatRequest, ChatResponse>, OpFlow<ChatRequest, ChatResponse> {

    /**
     * @return 智能体名称
     */
    String name();

    /**
     * @return 创建函数工具构建器
     */
    FunctionToolBuilder newFunctionToolBuilder();

    /**
     * 函数工具构建器
     */
    interface FunctionToolBuilder extends Buildable<ChatFunctionTool, FunctionToolBuilder> {

        /**
         * 设置函数名称
         *
         * @param name 函数名称
         * @return this
         */
        FunctionToolBuilder name(String name);

        /**
         * 设置函数摘要
         *
         * @param summary 函数摘要
         * @return this
         */
        FunctionToolBuilder summary(String summary);

    }

}
