package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * 工具应答消息
 * <p>
 * 由客户端在完成工具调用后发起，用于反馈工具调用结果<br/>
 * {@code Client > LLM}
 * </p>
 */
@JsonDeserialize
public final class ToolMessage extends Message {

    @JsonProperty("tool_call_id")
    private final String id;

    /**
     * 构造工具应答消息
     *
     * @param id      调用ID
     * @param content 应答结果
     */
    @JsonCreator
    public ToolMessage(

            @JsonProperty("tool_call_id")
            String id,

            @JsonProperty("content")
            String content

    ) {
        super(Role.TOOL, content);
        this.id = id;
    }

    /**
     * @return 调用ID
     */
    public String id() {
        return id;
    }

}
