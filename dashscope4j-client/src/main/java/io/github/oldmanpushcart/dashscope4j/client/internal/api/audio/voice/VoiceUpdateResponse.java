package io.github.oldmanpushcart.dashscope4j.client.internal.api.audio.voice;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.AlgoResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode(callSuper = true)
public class VoiceUpdateResponse extends AlgoResponse<Void> {

    Void output;

    @JsonCreator
    private VoiceUpdateResponse(

            @JacksonInject("dashscope/request")
            VoiceUpdateRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("desc")
            String desc,

            @JsonProperty("usage")
            Usage usage

    ) {
        super(request, uuid, code, desc, usage);
        this.output = null;
    }

}
