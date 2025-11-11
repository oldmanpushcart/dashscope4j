package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin.Plugin;

/**
 * 插件调用消息
 * <p>
 * 大模型侧调用完成后，作为补偿消息返回给客户端。<br/>
 * {@code LLM > Client}
 * </p>
 */
@JsonDeserialize
public final class PluginCallMessage extends Message {

    private final Plugin.Call call;

    @JsonCreator
    public PluginCallMessage(

            @JsonProperty("content")
            String content,

            @JsonProperty("plugin_call")
            Plugin.Call call

    ) {
        super(Role.PLUGIN, content);
        this.call = call;
    }

    public Plugin.Call call() {
        return call;
    }

}
