package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ToolCallMessage extends Message {

    @JsonProperty("tool_calls")
    List<Tool.Call> calls;

    public ToolCallMessage(String text, List<Tool.Call> calls) {
        super(Role.AI, Collections.singletonList(Content.ofText(text)));
        this.calls = calls;
    }

}
