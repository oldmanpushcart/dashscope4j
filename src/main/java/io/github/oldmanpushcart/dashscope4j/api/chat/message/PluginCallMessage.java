package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 插件调用消息
 * <p>
 * 大模型侧调用完成后，作为补偿消息返回给客户端。<br/>
 * {@code LLM > Client}
 * </p>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PluginCallMessage extends Message {

    /**
     * 插件调用存根
     */
    @JsonProperty("plugin_call")
    Plugin.Call call;

    /**
     * 构建插件调用消息
     *
     * @param text 消息文本
     *             <p>在插件调用场景下通常为空，这里保留主要考虑是未来可能扩展</p>
     * @param call 插件调用存根
     */
    public PluginCallMessage(String text, Plugin.Call call) {
        super(Role.AI, Content.ofText(text));
        this.call = call;
    }

}
