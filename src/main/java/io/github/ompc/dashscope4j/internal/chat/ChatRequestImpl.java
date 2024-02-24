package io.github.ompc.dashscope4j.internal.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public record ChatRequestImpl(Model model, Input input, Option option, Duration timeout) implements ChatRequest {

    private final static ObjectMapper mapper = JacksonUtils.mapper();
    private final static Logger logger = LoggerFactory.getLogger(ChatRequestImpl.class);

    @Override
    public String toString() {
        return "dashscope://chat";
    }

    @Override
    public HttpRequest newHttpRequest() {
        final var body = JacksonUtils.toJson(mapper, this);
        logger.debug("{}/{} => {}", this, model.name(), body);
        return HttpRequest.newBuilder()
                .uri(model().remote())
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    @Override
    public Function<String, ChatResponse> responseDeserializer() {
        return body -> {
            logger.debug("{}/{} <= {}", this, model.name(), body);
            return JacksonUtils.toObject(mapper, body, ChatResponseImpl.class);
        };
    }

}
