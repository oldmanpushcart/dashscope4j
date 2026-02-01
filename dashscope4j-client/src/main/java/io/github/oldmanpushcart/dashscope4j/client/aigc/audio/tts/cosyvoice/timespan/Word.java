package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Word(

        String text,
        Range index,
        Range time

) {

    @JsonCreator
    private Word(

            @JsonProperty("text")
            String text,

            @JsonProperty("begin_index")
            int beginIndex,

            @JsonProperty("end_index")
            int endIndex,

            @JsonProperty("begin_time")
            int beginTime,

            @JsonProperty("end_time")
            int endTime

    ) {
        this(
                text,
                new Range(beginIndex, endIndex),
                new Range(beginTime, endTime)
        );
    }

}
