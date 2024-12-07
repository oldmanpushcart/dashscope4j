package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collections;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PluginCallMessage extends Message {

    @JsonProperty("plugin_call")
    Plugin.Call call;

    public PluginCallMessage(String text, Plugin.Call call) {
        super(Role.AI, Collections.singletonList(Content.ofText(text)));
        this.call = call;
    }

}
