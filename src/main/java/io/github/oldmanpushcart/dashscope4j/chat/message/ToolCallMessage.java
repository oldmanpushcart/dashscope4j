package io.github.oldmanpushcart.dashscope4j.chat.message;

import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;

import java.util.List;

/**
 * 工具调用消息
 */
public interface ToolCallMessage extends Message {

    /**
     * @return 工具调用
     */
    List<Tool.Call> calls();

}
