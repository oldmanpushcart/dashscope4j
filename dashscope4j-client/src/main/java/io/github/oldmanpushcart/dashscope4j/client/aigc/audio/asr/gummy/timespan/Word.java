package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Word(

        @JsonProperty("begin_time")
        int beginAt,

        @JsonProperty("end_time")
        int endAt,

        @JsonProperty("text")
        String text,

        @JsonProperty("punctuation")
        String punctuation,

        @JsonProperty("fixed")
        boolean fixed

) {

}
