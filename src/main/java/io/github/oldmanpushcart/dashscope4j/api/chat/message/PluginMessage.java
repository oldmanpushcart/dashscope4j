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
public class PluginMessage extends Message {

    @JsonProperty
    String name;

    @JsonProperty
    Plugin.Status status;

    public PluginMessage(String text, String name, Plugin.Status status) {
        super(Role.PLUGIN, Collections.singletonList(Content.ofText(text)));
        this.name = name;
        this.status = status;
    }

}
