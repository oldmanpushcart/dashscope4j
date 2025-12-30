package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.openai.message;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.util.List;

public class OpenAiMessage {

    private final Message.Role role;
    private final List<Content<?>> contents;
    private final String reasoningContent;

    public OpenAiMessage(Message.Role role, List<Content<?>> contents, String reasoningContent) {
        this.role = role;
        this.contents = contents;
        this.reasoningContent = reasoningContent;
    }

}
