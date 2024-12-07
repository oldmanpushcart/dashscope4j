package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collections;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ToolMessage extends Message {

    @JsonProperty
    String name;

    public ToolMessage(String text, String name) {
        super(Role.TOOL, Collections.singletonList(Content.ofText(text)));
        this.name = name;
    }

}
