package io.github.oldmanpushcart.dashscope4j.api.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 音素时间片
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class PhonemeTimeSpan {

    int begin;
    int end;
    String text;
    int tone;

    /**
     * 构建音素时间片
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param text  标记文本
     * @param tone  音调记号
     *              <p>英文：0/1/2分别代表轻音/重音/次重音</p>
     *              <p>中文：1/2/3/4/5分别代表一声/二声/三声/四声/轻声</p>
     */
    @JsonCreator
    public PhonemeTimeSpan(

            @JsonProperty("begin_time")
            int begin,

            @JsonProperty("end_time")
            int end,

            @JsonProperty("text")
            String text,

            @JsonProperty("tone")
            int tone

    ) {
        this.begin = begin;
        this.end = end;
        this.text = text;
        this.tone = tone;
    }

}
