package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.util.List;

public class PluginCallMessageImpl extends MessageImpl {

    private final Call call;

    public PluginCallMessageImpl(String text, Call call) {
        super(Role.AI, List.of(Content.ofText(text)));
        this.call = call;
    }

    @JsonProperty("plugin_call")
    public Call call() {
        return call;
    }

    public record Call(
            @JsonProperty("name")
            String name,
            @JsonProperty("arguments")
            String arguments
    ) {
    }

}
