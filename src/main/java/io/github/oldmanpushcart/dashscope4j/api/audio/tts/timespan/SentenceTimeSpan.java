package io.github.oldmanpushcart.dashscope4j.api.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 整句时间片
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class SentenceTimeSpan {

    int begin;
    int end;
    List<WordTimeSpan> words;

    /**
     * 创建整句时间片
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param words 单字时间片集合
     */
    @JsonCreator
    public SentenceTimeSpan(

            @JsonProperty("begin_time")
            int begin,

            @JsonProperty("end_time")
            int end,

            @JsonProperty("words")
            List<WordTimeSpan> words

    ) {
        this.begin = begin;
        this.end = end;
        this.words = words;
    }

}
