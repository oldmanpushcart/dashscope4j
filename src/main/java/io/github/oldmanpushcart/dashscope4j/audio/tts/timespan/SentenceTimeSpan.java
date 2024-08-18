package io.github.oldmanpushcart.dashscope4j.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 整句时间片
 *
 * @param begin 开始偏移量(ms)
 * @param end   结束偏移量(ms)
 * @param words 单字时间片集合
 * @since 2.2.0
 */
public record SentenceTimeSpan(
        @JsonProperty("begin_time") int begin,
        @JsonProperty("end_time") int end,
        @JsonProperty("words") List<WordTimeSpan> words
) {

}
