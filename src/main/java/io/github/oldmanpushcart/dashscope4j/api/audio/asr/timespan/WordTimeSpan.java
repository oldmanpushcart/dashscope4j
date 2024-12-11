package io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 单字时间片
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class WordTimeSpan {

    int begin;
    int end;
    String text;
    String punctuation;

    /**
     * 单字时间片
     *
     * @param begin       开始偏移量(ms)
     * @param end         结束偏移量(ms)
     * @param text        单字文本
     * @param punctuation 标点符号
     */
    @JsonCreator
    public WordTimeSpan(

            @JsonProperty("begin_time")
            int begin,

            @JsonProperty("end_time")
            int end,

            @JsonProperty("text")
            String text,

            @JsonProperty("punctuation")
            String punctuation

    ) {
        this.begin = begin;
        this.end = end;
        this.text = text;
        this.punctuation = punctuation;
    }

}
