package io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.plaintext;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class PlaintextChatRequest extends ChatRequest {

    public PlaintextChatRequest(ChatRequest chatRequest) {
        super(ChatRequest.newBuilder(chatRequest));
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{
            put("messages", new ArrayList<>() {{
                messages().forEach(message -> {
                    if (message instanceof UserMessage
                            || message instanceof AssistantMessage
                            || message instanceof SystemMessage) {
                        add(new HashMap<>() {{
                            put("role", message.role());
                            put("content", message.text());
                        }});
                    } else {
                        add(message);
                    }
                });
            }});
        }};
    }

}
