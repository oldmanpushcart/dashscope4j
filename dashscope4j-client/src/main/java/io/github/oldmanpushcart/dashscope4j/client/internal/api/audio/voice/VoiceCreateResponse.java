package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.voice;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VoiceCreateResponse extends AlgoResponse<VoiceCreateResponse.Output> {

    Output output;

    @JsonCreator
    private VoiceCreateResponse(

            @JacksonInject("dashscope/request")
            VoiceCreateRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("desc")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ){
        super(request, uuid, code, desc, usage);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Output {

        @JsonProperty("voice_id")
        String voiceId;

    }

}
