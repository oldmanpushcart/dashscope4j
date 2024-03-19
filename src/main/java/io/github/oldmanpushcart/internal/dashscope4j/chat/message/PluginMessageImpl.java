package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.PluginMessage;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;

import java.util.List;

public class PluginMessageImpl extends MessageImpl implements PluginMessage {

    private final String name;
    private final Plugin.Status status;

    public PluginMessageImpl(String text, String name, Plugin.Status status) {
        super(Role.PLUGIN, List.of(Content.ofText(text)));
        this.name = name;
        this.status = status;
    }

    @JsonProperty("name")
    @Override
    public String name() {
        return name;
    }

    @JsonProperty("status")
    @Override
    public Plugin.Status status() {
        return status;
    }

}
