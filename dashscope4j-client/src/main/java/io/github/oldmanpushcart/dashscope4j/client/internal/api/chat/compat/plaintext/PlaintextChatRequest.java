package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.plaintext;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class PlaintextChatRequest extends ChatRequest {

    protected PlaintextChatRequest(Builder builder) {
        super(builder);
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PlaintextChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ChatRequest.Builder {

        public Builder() {
            super();
        }

        public Builder(PlaintextChatRequest request) {
            super(request);
        }

        @Override
        public PlaintextChatRequest build() {
            return new PlaintextChatRequest(this);
        }

    }

}
