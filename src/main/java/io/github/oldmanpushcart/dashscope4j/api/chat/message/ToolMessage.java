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
     * 工具名称
     */
    @JsonProperty
    String name;

    /**
     * 构造工具应答消息
     *
     * @param text 应答结果
     * @param name 工具名称
     */
    @JsonCreator
    public ToolMessage(

            @JsonProperty("content")
            String text,

            @JsonProperty("name")
            String name

    ) {
        super(Role.TOOL, Content.ofText(text));
        this.name = name;
    }

}
