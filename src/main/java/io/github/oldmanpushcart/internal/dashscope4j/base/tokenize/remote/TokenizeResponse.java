package io.github.oldmanpushcart.internal.dashscope4j.base.tokenize.remote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.util.List;

public record TokenizeResponse(String uuid, Ret ret, Usage usage, Output output)
        implements ApiResponse<TokenizeResponse.Output> {

    public record Output(

            @JsonProperty("token_ids")
            List<Integer> tokenIds,

            @JsonProperty("tokens")
            List<String> tokens,

            @JsonProperty("input_tokens")
            int total

    ) implements ApiResponse.Output {

    }

    @JsonCreator
    public static TokenizeResponse of(

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
        return new TokenizeResponse(
                uuid,
                Ret.of(code, message),
                usage,
                output
        );
    }

}
