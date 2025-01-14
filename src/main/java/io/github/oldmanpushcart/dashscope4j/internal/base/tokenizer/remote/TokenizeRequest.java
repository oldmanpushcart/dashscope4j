package io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.remote;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class TokenizeRequest extends AlgoRequest<ChatModel, TokenizeResponse> {

    List<Message> messages;

    private TokenizeRequest(Builder builder) {
        super(TokenizeResponse.class, builder);
        this.messages = unmodifiableList(builder.messages);
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("messages", new ArrayList<Object>() {{
                for (final Message message : messages) {
                    add(new ObjectMap() {{
                        put("role", message.role());
                        put("content", message.text());
                    }});
                }
            }});
        }};
    }

    @Override
    public Request newHttpRequest() {
        return new Request.Builder(super.newHttpRequest())
                .url("https://dashscope.aliyuncs.com/api/v1/tokenizer")
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TokenizeRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<ChatModel, TokenizeRequest, Builder> {

        private ChatModel model;
        private final List<Message> messages = new ArrayList<>();

        private Builder() {

        }

        private Builder(TokenizeRequest request) {
            super(request);
            this.messages.addAll(request.messages);
        }

        /**
         * 添加消息
         *
         * @param message 消息
         * @return this
         */
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }

        /**
         * 添加消息列表
         *
         * @param messages 消息列表
         * @return this
         */
        public Builder addMessages(Collection<Message> messages) {
            this.messages.addAll(messages);
            return this;
        }

        @Override
        public TokenizeRequest build() {
            return new TokenizeRequest(this);
        }

    }

}
