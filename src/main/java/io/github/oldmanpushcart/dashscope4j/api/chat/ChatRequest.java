package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.JacksonUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ChatRequest extends ApiRequest<ChatModel, ChatResponse> {

    private final List<Message> messages;

    private ChatRequest(Builder builder) {
        super(ChatResponse.class, builder);
        this.messages = builder.messages;
    }

    @Override
    protected Object input() {
        return new HashMap<Object, Object>() {{
            put("messages", encodeMessages());
        }};
    }

    private List<JsonNode> encodeMessages() {
        final ChatModel.Mode mode = model().mode();
        final List<JsonNode> nodes = new LinkedList<>();
        messages.forEach(message -> {
            final JsonNode messageNode = JacksonUtils.toNode(message);
            if (messageNode instanceof ObjectNode) {
                final ObjectNode node = (ObjectNode) messageNode;
                switch (mode) {
                    case TEXT:
                        node.put("content", message.text());
                        break;
                    case MULTIMODAL:
                        node.putPOJO("content", message.contents());
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported mode: " + mode);
                }
            }
            nodes.add(messageNode);
        });
        return nodes;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<ChatModel, ChatRequest, Builder> {

        private final List<Message> messages;

        protected Builder() {
            this.messages = new LinkedList<>();
        }

        protected Builder(ChatRequest request) {
            super(request);
            this.messages = new LinkedList<>(request.messages);
        }

        public Builder appendMessage(Message message) {
            this.messages.add(message);
            return this;
        }

        public Builder appendMessages(Collection<Message> messages) {
            this.messages.addAll(messages);
            return this;
        }

        @Override
        public ChatRequest build() {
            return new ChatRequest(this);
        }

    }

}
