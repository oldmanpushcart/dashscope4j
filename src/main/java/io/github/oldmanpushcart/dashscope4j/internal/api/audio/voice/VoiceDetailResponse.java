package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VoiceDetailResponse extends AlgoResponse<VoiceDetailResponse.Output> {

    Output output;

    @JsonCreator
    private VoiceDetailResponse(

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

    ) {
        super(uuid, code, desc, usage);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Output {

        String target;
        Instant createdAt;
        Instant updatedAt;
        URI resource;

        @JsonCreator
        private Output(

                @JsonProperty("target_model")
                String target,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_create")
                Date createdAt,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_modified")
                Date updatedAt,

                @JsonProperty("resource_link")
                String resourceLink

        ) {
            this.target = target;
            this.createdAt = createdAt.toInstant();
            this.updatedAt = updatedAt.toInstant();
            this.resource = URI.create(resourceLink);
        }

    }

}
