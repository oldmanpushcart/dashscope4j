package io.github.oldmanpushcart.dashscope4j.chat.message;

import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;

/**
 * 插件调用消息
 */
public interface PluginCallMessage extends Message {

    /**
     * @return 插件调用
     */
    Plugin.Call call();

}
