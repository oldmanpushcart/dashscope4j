package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode(callSuper = true)
public class VocabularyUpdateResponse extends AlgoResponse<Void> {

    Void output;

    @JsonCreator
    private VocabularyUpdateResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("desc")
            String desc,

            @JsonProperty("usage")
            Usage usage

    ) {
        super(uuid, code, desc, usage);
        this.output = null;
    }

}
