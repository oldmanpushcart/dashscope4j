package io.github.oldmanpushcart.dashscope4j.client.api.audio.asr;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.audio.asr.timespan.SentenceTimeSpan;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * 语音识别应答
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RecognitionResponse extends AlgoResponse<RecognitionResponse.Output> {

    Output output;

    @JsonCreator
    private RecognitionResponse(

            @JacksonInject("dashscope/request")
            RecognitionRequest request,

            @JacksonInject("http/header/x-request-id")
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
        super(request, uuid, code, desc, usage);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Output {

        @JsonProperty
        SentenceTimeSpan sentence;

    }

}
