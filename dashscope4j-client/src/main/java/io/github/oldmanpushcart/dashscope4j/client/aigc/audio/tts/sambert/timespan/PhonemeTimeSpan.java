package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 音素时间段
 */
public record PhonemeTimeSpan(

        @JsonProperty("begin_time")
        int begin,

        @JsonProperty("end_time")
        int end,

        @JsonProperty("text")
        String text,

        @JsonProperty("tone")
        int tone

) {
}
