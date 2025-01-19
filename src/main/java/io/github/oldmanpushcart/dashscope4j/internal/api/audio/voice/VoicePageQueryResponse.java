package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary.VocabularyPageQueryResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static java.util.Collections.unmodifiableList;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode(callSuper = true)
public class VoicePageQueryResponse extends AlgoResponse<VoicePageQueryResponse.Output> {

    Output output;

    @JsonCreator
    private VoicePageQueryResponse(

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

        List<Item> items;

        @JsonCreator
        private Output(
                @JsonProperty("voice_list")
                List<Item> items
        ) {
            this.items = unmodifiableList(items);
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Item {

        String voiceId;
        Instant createdAt;
        Instant updatedAt;

        @JsonCreator
        private Item(

                @JsonProperty("voice_id")
                String voiceId,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_create")
                Date createdAt,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_modified")
                Date updatedAt

        ) {
            this.voiceId = voiceId;
            this.createdAt = createdAt.toInstant();
            this.updatedAt = updatedAt.toInstant();
        }

    }

}
