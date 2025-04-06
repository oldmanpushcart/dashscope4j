package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 工具调用消息
 * <p>
 * 由大模型侧发起，表明大模型期望调用本地工具。<br/>
 * {@code LLM > Client}
 * </p>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ToolCallMessage extends Message {

    /**
     * 工具调用存根集合
     */
    @JsonProperty("tool_calls")
    List<Tool.Call> calls;

    @JsonCreator
    public ToolCallMessage(

            @JsonProperty("content")
            String text,

            @JsonProperty("tool_calls")
            List<Tool.Call> calls

    ) {
        super(Role.AI, Content.ofText(text));
        this.calls = calls;
    }

}
