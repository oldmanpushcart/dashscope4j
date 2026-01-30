package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SentenceTimeSpan(

        @JsonProperty("begin_time")
        int begin,

        @JsonProperty("end_time")
        int end,

        @JsonProperty("words")
        List<WordTimeSpan> words

) {
}
