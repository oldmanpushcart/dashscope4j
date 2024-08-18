package io.github.oldmanpushcart.dashscope4j.audio.asr.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 整句时间片
 *
 * @param begin 开始偏移量(ms)
 * @param end   结束偏移量(ms)
 * @param text  句子内容
 * @param words 单字时间片集合
 * @since 2.2.0
 */
public record SentenceTimeSpan(
        @JsonProperty("begin_time") int begin,
        @JsonProperty("end_time") int end,
        @JsonProperty("text") String text,
        @JsonProperty("words") List<WordTimeSpan> words
) {

    /**
     * @return 是否整句
     */
    public boolean isEnd() {
        return end > 0;
    }

}
