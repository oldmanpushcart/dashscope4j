package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

/**
 * 智能体
 */
public interface ChatAgent extends ChatOp {

    /**
     * @return 智能体名称
     */
    String name();

    /**
     * @return 智能体描述
     */
    String description();

    /**
     * @return 客户端
     */
    DashscopeClient client();

    /**
     * @return 创建函数工具构建器
     */
    FunctionToolBuilder newFunctionToolBuilder();

    /**
     * 函数工具构建器
     */
    interface FunctionToolBuilder extends Buildable<FunctionTool, FunctionToolBuilder> {

        /**
         * 设置函数名称
         *
         * @param name 函数名称
         * @return this
         */
        FunctionToolBuilder name(String name);

        /**
         * 设置函数描述
         *
         * @param summary 函数描述
         * @return this
         */
        FunctionToolBuilder description(String summary);

    }

}
