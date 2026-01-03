package io.github.oldmanpushcart.dashscope4j.client.api.chat.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;

import java.util.List;


/**
 * 消息
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "role"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
        @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
        @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
        @JsonSubTypes.Type(value = ToolMessage.class, name = "tool")
})
public sealed interface Message permits SystemMessage, AssistantMessage, UserMessage, ToolMessage {

    /**
     * @return 角色
     */
    @JsonProperty("role")
    Role role();

    /**
     * @return 文本内容
     */
    String text();

    /**
     * 角色
     */
    enum Role {

        /**
         * 系统
         */
        @JsonProperty("system")
        SYSTEM,

        /**
         * AI
         */
        @JsonProperty("assistant")
        AI,

        /**
         * 用户
         */
        @JsonProperty("user")
        USER,

        /**
         * 工具
         */
        @JsonProperty("tool")
        TOOL

    }

    static SystemMessage system(String content) {
        return SystemMessage.newBuilder()
                .addContent(Content.text(content))
                .build();
    }

    static SystemMessage system(Content content) {
        return SystemMessage.newBuilder()
                .addContent(content)
                .build();
    }

    static UserMessage user(String content) {
        return UserMessage.newBuilder()
                .addContent(Content.text(content))
                .build();
    }

    static UserMessage user(Content content) {
        return UserMessage.newBuilder()
                .addContent(content)
                .build();
    }

    static UserMessage user(List<Content> contents) {
        return UserMessage.newBuilder()
                .addContents(contents)
                .build();
    }

    static AssistantMessage assistant(String content) {
        return AssistantMessage.newBuilder()
                .addContent(Content.text(content))
                .build();
    }

    static AssistantMessage assistant(String content, boolean partial) {
        return AssistantMessage.newBuilder()
                .addContent(Content.text(content))
                .partial(partial)
                .build();
    }


}
