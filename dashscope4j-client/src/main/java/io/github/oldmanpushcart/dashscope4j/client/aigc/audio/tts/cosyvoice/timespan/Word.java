package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Word(

        @JsonProperty("text")
        String text,

        @JsonProperty("begin_index")
        int beginIndex,

        @JsonProperty("end_index")
        int endIndex,

        @JsonProperty("begin_time")
        int beginAt,

        @JsonProperty("end_time")
        int endAt

) {

}
