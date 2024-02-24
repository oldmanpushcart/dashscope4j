package io.github.ompc.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.algo.AlgoResponse;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

import java.net.URI;
import java.util.List;

public class GenImageResponse extends AlgoResponse<GenImageResponse.Output> {

    private GenImageResponse(String uuid, Ret ret, Usage usage, Output output) {
        super(uuid, ret, usage, output);
    }

    public record Output(List<Item> results) implements ApiResponse.Output {

        @JsonCreator
        static Output of(
                @JsonProperty("results")
                List<Item> results
        ) {
            return new Output(results);
        }

    }

    public record Item(Ret ret, URI image) {

        @JsonCreator
        static Item of(
                @JsonProperty("url")
                String url,
                @JsonProperty("code")
                String code,
                @JsonProperty("message")
                String message
        ) {
            return new Item(Ret.of(code, message), URI.create(url));
        }

    }

    @JsonCreator
    static GenImageResponse of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            Output output
    ) {
        return new GenImageResponse(uuid, Ret.of(code, message), usage, output);
    }

}
