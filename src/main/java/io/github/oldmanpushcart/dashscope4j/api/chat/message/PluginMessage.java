package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 插件应答消息
 * <p>
 * 大模型侧调用完成后，作为补偿消息返回给客户端。<br/>
 * {@code LLM > Client}
 * </p>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PluginMessage extends Message {

    /**
     * 插件名称
     */
    @JsonProperty
    String name;

    /**
     * 插件应答状态
     */
    @JsonProperty
    Plugin.Status status;

    /**
     * 构造插件应答消息
     *
     * @param text   插件应答结果
     * @param name   插件名称
     * @param status 插件应答状态
     */
    public PluginMessage(String text, String name, Plugin.Status status) {
        super(Role.PLUGIN, Content.ofText(text));
        this.name = name;
        this.status = status;
    }

}
