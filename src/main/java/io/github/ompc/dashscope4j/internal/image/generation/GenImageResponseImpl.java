package io.github.ompc.dashscope4j.internal.image.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;

import java.net.URI;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public record GenImageResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements GenImageResponse {

    @JsonCreator
    static GenImageResponseImpl of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            OutputImpl output
    ) {
        return new GenImageResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

    public record OutputImpl(List<Item> results) implements Output {

        @JsonCreator
        static OutputImpl of(
                @JsonProperty("results")
                List<ItemImpl> results
        ) {
            return new OutputImpl(unmodifiableList(results));
        }

    }

    public record ItemImpl(Ret ret, URI image) implements Item {

        @JsonCreator
        static ItemImpl of(
                @JsonProperty("url")
                String url,
                @JsonProperty("code")
                String code,
                @JsonProperty("message")
                String message
        ) {
            return new ItemImpl(Ret.of(code, message), URI.create(url));
        }

    }

}
