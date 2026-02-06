package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Word(

        Range range,
        String text,
        String punctuation

) {

    @JsonCreator
    private Word(

            @JsonProperty("begin_time")
            int beginTime,

            @JsonProperty("end_time")
            int endTime,

            @JsonProperty("text")
            String text,

            @JsonProperty("punctuation")
            String punctuation

    ) {
        this(new Range(beginTime, endTime), text, punctuation);
    }

}
