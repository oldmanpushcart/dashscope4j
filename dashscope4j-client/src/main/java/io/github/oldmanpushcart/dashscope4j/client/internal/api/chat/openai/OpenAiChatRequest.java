package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.openai;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;

import java.util.ArrayList;
import java.util.List;

public class OpenAiChatRequest extends ApiRequest<OpenAiChatResponse> {

    private final ChatModel model;
    private final List<Message> messages;
    private final Parameters parameters;

    protected OpenAiChatRequest(Builder builder) {
        super(OpenAiChatResponse.class, builder);
        this.model = builder.model;
        this.messages = builder.messages;
        this.parameters = builder.parameters;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(OpenAiChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<OpenAiChatRequest, Builder> {

        private ChatModel model;
        private final List<Message> messages = new ArrayList<>();
        private final Parameters parameters = new Parameters();

        public Builder() {

        }

        public Builder(OpenAiChatRequest request) {
            super(request);
        }

        @Override
        public OpenAiChatRequest build() {
            return new OpenAiChatRequest(this);
        }

    }

}
