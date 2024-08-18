package io.github.oldmanpushcart.dashscope4j.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 单字时间片
 *
 * @param begin    开始偏移量(ms)
 * @param end      结束偏移量(ms)
 * @param text     单字文本
 * @param phonemes 音素时间片集合
 * @since 2.2.0
 */
public record WordTimeSpan(
        @JsonProperty("begin_time") int begin,
        @JsonProperty("end_time") int end,
        @JsonProperty("text") String text,
        @JsonProperty("phonemes") List<PhonemeTimeSpan> phonemes
) {

}
