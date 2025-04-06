package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;


/**
 * <pre><code>
 * {
 *  "output":{
 *      "vocabulary_id": "vocab-test-02d4d2612c71442fab8a2695fc008341"
 *  },
 *  "usage": {
 *      "count": 1
 *  },
 *  "request_id": "8f6de4fa-261c-9bf0-b643-8eee27b1c0fc"
 * }
 * </code></pre>
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VocabularyCreateResponse extends AlgoResponse<VocabularyCreateResponse.Output> {

    Output output;

    @JsonCreator
    private VocabularyCreateResponse(

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
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Output {

        @JsonProperty("vocabulary_id")
        String vocabularyId;

    }

}
