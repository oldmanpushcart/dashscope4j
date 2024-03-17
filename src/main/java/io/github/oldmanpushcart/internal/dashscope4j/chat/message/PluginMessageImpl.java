package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;

import java.util.List;

public class PluginMessageImpl extends MessageImpl {

    private final String name;
    private final Status status;

    public PluginMessageImpl(String text, String name, Status status) {
        super(Role.PLUGIN, List.of(Content.ofText(text)));
        this.name = name;
        this.status = status;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }

    @JsonProperty("status")
    public Status status() {
        return status;
    }

    public record Status(
            @JsonProperty("code")
            int code,
            @JsonProperty("name")
            String name,
            @JsonProperty("message")
            String message
    ) {
    }

}
