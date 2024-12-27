package io.github.oldmanpushcart.dashscope4j.api.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.List;

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
    List<PhonemeTimeSpan> phonemes;

    /**
     * 创建单字时间片
     *
     * @param begin    开始偏移量(ms)
     * @param end      结束偏移量(ms)
     * @param text     单字文本
     * @param phonemes 音素时间片集合
     */
    @JsonCreator
    public WordTimeSpan(

            @JsonProperty("begin_time")
            int begin,

            @JsonProperty("end_time")
            int end,

            @JsonProperty("text")
            String text,

            @JsonProperty("phonemes")
            List<PhonemeTimeSpan> phonemes

    ) {
        this.begin = begin;
        this.end = end;
        this.text = text;
        this.phonemes = phonemes;
    }

}
