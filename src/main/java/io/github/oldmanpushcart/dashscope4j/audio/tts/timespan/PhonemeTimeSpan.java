package io.github.oldmanpushcart.dashscope4j.audio.tts.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 音素时间片
 *
 * @param begin 开始偏移量(ms)
 * @param end   结束偏移量(ms)
 * @param text  标记文本
 * @param tone  音调记号
 *              <p>英文：0/1/2分别代表轻音/重音/次重音</p>
 *              <p>中文：1/2/3/4/5分别代表一声/二声/三声/四声/轻声</p>
 * @since 2.2.0
 */
public record PhonemeTimeSpan(
        @JsonProperty("begin_time") int begin,
        @JsonProperty("end_time") int end,
        @JsonProperty("text") String text,
        @JsonProperty("tone") int tone
) {

}
