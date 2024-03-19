package io.github.oldmanpushcart.dashscope4j.chat.message;

/**
 * 工具调用结果消息
 *
 * @since 1.2.0
 */
public interface ToolMessage extends Message {

    /**
     * @return 工具名称
     */
    String name();

}
