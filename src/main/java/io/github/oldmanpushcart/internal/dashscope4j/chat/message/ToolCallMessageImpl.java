package io.github.oldmanpushcart.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.chat.tool.Tool;

import java.util.List;

public class ToolCallMessageImpl extends MessageImpl implements ToolCallMessage {

    private final List<Tool.Call> calls;

    public ToolCallMessageImpl(String text, List<Tool.Call> calls) {
        super(Role.AI, List.of(Content.ofText(text)));
        this.calls = calls;
    }

    @JsonProperty("tool_calls")
    @Override
    public List<Tool.Call> calls() {
        return calls;
    }

}
