package io.github.oldmanpushcart.dashscope4j.internal.base.tokenizer.remote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Collections.unmodifiableList;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TokenizeResponse extends AlgoResponse<TokenizeResponse.Output> {

    Output output;

    @JsonCreator
    public TokenizeResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(uuid, code, desc, usage);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    public static class Output {

        List<Integer> tokenIds;
        List<String> tokens;
        int total;

        @JsonCreator
        public Output(

                @JsonProperty("token_ids")
                List<Integer> tokenIds,

                @JsonProperty("tokens")
                List<String> tokens,

                @JsonProperty("input_tokens")
                int total

        ) {
            this.tokenIds = unmodifiableList(tokenIds);
            this.tokens = unmodifiableList(tokens);
            this.total = total;
        }

    }

}
