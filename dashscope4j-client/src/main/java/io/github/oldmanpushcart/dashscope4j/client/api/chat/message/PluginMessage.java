package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.plugin.Plugin;

/**
 * 插件应答消息
 * <p>
 * 大模型侧调用完成后，作为补偿消息返回给客户端。<br/>
 * {@code LLM > Client}
 * </p>
 */
@JsonDeserialize
public final class PluginMessage extends Message {

    private final String name;
    private final Plugin.Status status;

    @JsonCreator
    public PluginMessage(

            @JsonProperty("content")
            String content,

            @JsonProperty("name")
            String name,

            @JsonProperty("status")
            Plugin.Status status

    ) {
        super(Role.PLUGIN, content);
        this.name = name;
        this.status = status;
    }

    public String name() {
        return name;
    }

    public Plugin.Status status() {
        return status;
    }

}
