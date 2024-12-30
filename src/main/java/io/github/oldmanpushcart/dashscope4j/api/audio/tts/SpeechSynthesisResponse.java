package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * 语音合成应答
 */
@Getter
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class SpeechSynthesisResponse extends AlgoResponse<SpeechSynthesisResponse.Output> {

    private final Output output;

    @JsonCreator
    private SpeechSynthesisResponse(

            @JacksonInject("header/x-request-id")
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
    @ToString
    @EqualsAndHashCode
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Output {

    }

}
