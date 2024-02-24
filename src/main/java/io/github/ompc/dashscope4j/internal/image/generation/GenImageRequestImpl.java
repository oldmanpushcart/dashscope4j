package io.github.ompc.dashscope4j.internal.image.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;

public record GenImageRequestImpl(
        Model model,
        InputImpl input,
        Option option,
        Duration timeout
) implements GenImageRequest {

    private final static ObjectMapper mapper = JacksonUtils.mapper();
    private final static Logger logger = LoggerFactory.getLogger(GenImageRequestImpl.class);

    @Override
    public String toString() {
        return "dashscope://image/generation";
    }

    public record InputImpl(String prompt,String negative) implements Input {

        @JsonCreator
        static InputImpl of(
                @JsonProperty("prompt")
                String prompt,
                @JsonProperty("negative")
                String negative
        ) {
            return new InputImpl(prompt, negative);
        }

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
    public Function<String, GenImageResponse> responseDeserializer() {
        return body -> {
            logger.debug("{}/{} <= {}", this, model.name(), body);
            return JacksonUtils.toObject(mapper, body, GenImageResponseImpl.class);
        };
    }

}
