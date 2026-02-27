package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 单词时间段
 */
public record WordTimeSpan(

        @JsonProperty("begin_time")
        int beginAt,

        @JsonProperty("end_time")
        int endAt,

        @JsonProperty("text")
        String text,

        @JsonProperty("phonemes")
        List<PhonemeTimeSpan> phonemes

) {
}
