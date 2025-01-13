package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
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
public class VocabularyPageQueryResponse extends AlgoResponse<VocabularyPageQueryResponse.Output> {

    Output output;

    @JsonCreator
    private VocabularyPageQueryResponse(

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
                @JsonProperty("vocabulary_list")
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

        String vocabularyId;
        Instant createdAt;
        Instant updatedAt;

        @JsonCreator
        private Item(

                @JsonProperty("vocabulary_id")
                String vocabularyId,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_create")
                Date createdAt,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                @JsonProperty("gmt_modified")
                Date updatedAt

        ) {
            this.vocabularyId = vocabularyId;
            this.createdAt = createdAt.toInstant();
            this.updatedAt = updatedAt.toInstant();
        }

    }

}
