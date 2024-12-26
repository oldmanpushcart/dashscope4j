package io.github.oldmanpushcart.dashscope4j.api.image.generation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.List;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GenImageResponse extends AlgoResponse<GenImageResponse.Output> {

    Output output;

    @JsonCreator
    public GenImageResponse(

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
        super(uuid, code, message, usage);
        this.output = output;
    }

    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Output {

        @JsonProperty("results")
        List<Item> results;

    }

    @Getter
    @Accessors(fluent = true)
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Item extends Ret {

        URI image;

        @JsonCreator
        public Item(

                @JsonProperty("code")
                String code,

                @JsonProperty("message")
                String desc,

                @JsonProperty("url")
                String url

        ) {
            super(code, desc);
            this.image = URI.create(url);
        }

    }

}
