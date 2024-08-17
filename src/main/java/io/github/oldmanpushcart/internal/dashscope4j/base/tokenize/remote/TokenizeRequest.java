package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.ApiRequestBuilderImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.HEADER_CONTENT_TYPE;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

public class TokenizeRequest implements HttpApiRequest<TokenizeResponse> {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final Duration timeout;
    private final ChatModel model;
    private final List<Message> messages;

    public TokenizeRequest(Builder builder) {
        this.timeout = builder.timeout;
        this.model = builder.model;
        this.messages = builder.messages;
    }

    @Override
    public String suite() {
        return "dashscope://tokenize";
    }

    @Override
    public String type() {
        return model.name();
    }

    @Override
    public Duration timeout() {
        return timeout;
    }

    @JsonProperty("model")
    public ChatModel model() {
        return model;
    }

    public List<Message> messages() {
        return messages;
    }

    @JsonProperty("input")
    protected Object input() {
        return new HashMap<>() {{
            put("messages", new ArrayList<>() {{
                for (final var message : messages) {
                    add(new HashMap<>() {{
                        put("role", message.role());
                        put("content", message.text());
                    }});
                }
            }});
        }};
    }

    @Override
    public HttpRequest newHttpRequest() {

        final var body = JacksonUtils.toJson(this);
        logger.debug("{} => {}", protocol(), body);
        return HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tokenizer"))
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Override
    public Function<String, TokenizeResponse> newResponseDecoder() {
        return body -> {
            logger.debug("{} <= {}", protocol(), body);
            return JacksonUtils.toObject(body, TokenizeResponse.class);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(TokenizeRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequestBuilderImpl<TokenizeRequest, Builder> {

        private Duration timeout;
        private ChatModel model;
        private final List<Message> messages = new ArrayList<>();

        protected Builder() {

        }

        protected Builder(TokenizeRequest request) {
            this.timeout = request.timeout;
            this.model = request.model;
            updateList(REPLACE_ALL, this.messages, request.messages());
        }

        @Override
        public Builder timeout(Duration timeout) {
            this.timeout = requireNonNull(timeout);
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = requireNonNull(model);
            return this;
        }

        public Builder messages(List<Message> messages) {
            requireNonNull(messages);
            updateList(REPLACE_ALL, this.messages, messages);
            return this;
        }

        @Override
        public TokenizeRequest build() {
            requireNonNull(model, "model is required");
            check(model, v -> v.mode() == ChatModel.Mode.TEXT, "model must be text model!");
            check(messages, CollectionUtils::isNotEmptyCollection, "messages is empty!");
            return new TokenizeRequest(this);
        }

    }

}
