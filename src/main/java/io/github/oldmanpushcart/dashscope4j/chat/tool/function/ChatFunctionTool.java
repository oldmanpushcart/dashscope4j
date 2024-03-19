package io.github.oldmanpushcart.dashscope4j.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;

/**
 * 对话函数工具
 *
 * @since 1.2.0
 */
public interface ChatFunctionTool extends Tool {

    /**
     * 对话函数调用
     *
     * @since 1.2.0
     */
    interface Call extends Tool.Call {

        /**
         * @return 函数名称
         */
        String name();

        /**
         * @return 函数参数
         */
        String arguments();

    }

}
