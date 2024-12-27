package io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

import static java.util.Collections.unmodifiableList;

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
    String text;
    List<WordTimeSpan> words;

    /**
     * 构造函数
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param text  整句内容
     * @param words 整句中的单字时间片集合
     */
    @JsonCreator
    public SentenceTimeSpan(

            @JsonProperty("begin_time")
            int begin,

            @JsonProperty("end_time")
            int end,

            @JsonProperty("text")
            String text,

            @JsonProperty("words")
            List<WordTimeSpan> words

    ) {
        this.begin = begin;
        this.end = end;
        this.text = text;
        this.words = unmodifiableList(words);
    }

    /**
     * @return 是否整句
     */
    public boolean isEnd() {
        return end > 0;
    }

}
