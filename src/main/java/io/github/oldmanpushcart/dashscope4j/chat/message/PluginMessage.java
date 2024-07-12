package io.github.oldmanpushcart.dashscope4j.chat.message;

import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;

/**
 * 插件应答消息
 */
public interface PluginMessage extends Message {

    /**
     * @return 插件名称
     */
    String name();

    /**
     * @return 插件调用结果状态
     */
    Plugin.Status status();

}
