package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.PluginCallMessage;
import io.github.oldmanpushcart.dashscope4j.chat.plugin.Plugin;

import java.util.List;

public class PluginCallMessageImpl extends MessageImpl implements PluginCallMessage {

    private final Plugin.Call call;

    public PluginCallMessageImpl(String text, Plugin.Call call) {
        super(Role.AI, List.of(Content.ofText(text)));
        this.call = call;
    }

    @JsonProperty("plugin_call")
    @Override
    public Plugin.Call call() {
        return call;
    }

}
