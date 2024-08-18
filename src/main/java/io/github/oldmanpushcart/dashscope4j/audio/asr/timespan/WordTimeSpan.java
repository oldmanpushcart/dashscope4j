package io.github.oldmanpushcart.dashscope4j.audio.asr.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 单字时间片
 *
 * @param begin       开始偏移量(ms)
 * @param end         结束偏移量(ms)
 * @param text        单字文本
 * @param punctuation 标点符号
 * @since 2.2.0
 */
public record WordTimeSpan(
        @JsonProperty("begin_time") int begin,
        @JsonProperty("end_time") int end,
        @JsonProperty("text") String text,
        @JsonProperty("punctuation") String punctuation
) {

}
