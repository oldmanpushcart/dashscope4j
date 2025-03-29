package io.github.oldmanpushcart.dashscope4j.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 工具应答消息
 * <p>
 * 由客户端在完成工具调用后发起，用于反馈工具调用结果<br/>
 * {@code Client > LLM}
 * </p>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ToolMessage extends Message {

    /**
     * 调用ID
     *
     * @since 3.1.0
     */
    @JsonProperty("tool_call_id")
    String id;

    /**
     * 构造工具应答消息
     *
     * @param id   调用ID
     * @param text 应答结果
     */
    @JsonCreator
    public ToolMessage(

            @JsonProperty("tool_call_id")
            String id,

            @JsonProperty("content")
            String text

    ) {
        super(Role.TOOL, Content.ofText(text));
        this.id = id;
    }

}
