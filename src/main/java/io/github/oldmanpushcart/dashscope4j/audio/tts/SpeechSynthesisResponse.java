package io.github.oldmanpushcart.dashscope4j.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoResponse;

import java.util.List;

/**
 * 语音合成应答
 * @since 2.2.0
 */
public interface SpeechSynthesisResponse extends ExchangeAlgoResponse<SpeechSynthesisResponse.Output> {

    /**
     * 应答数据
     */
    interface Output {

        /**
         * @return 整句时间片
         */
        SentenceTimeSpan sentence();

    }

    /**
     * 音素时间片
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param text  标记文本
     * @param tone  音调记号
     *              <p>英文：0/1/2分别代表轻音/重音/次重音</p>
     *              <p>中文：1/2/3/4/5分别代表一声/二声/三声/四声/轻声</p>
     */
    record PhonemeTimeSpan(
            @JsonProperty("begin_time") int begin,
            @JsonProperty("end_time") int end,
            @JsonProperty("text") String text,
            @JsonProperty("tone") int tone
    ) {

    }

    /**
     * 单字时间片
     *
     * @param begin    开始偏移量(ms)
     * @param end      结束偏移量(ms)
     * @param text     单字文本
     * @param phonemes 音素时间片集合
     */
    record WordTimeSpan(
            @JsonProperty("begin_time") int begin,
            @JsonProperty("end_time") int end,
            @JsonProperty("text") String text,
            @JsonProperty("phonemes") List<PhonemeTimeSpan> phonemes
    ) {

    }

    /**
     * 整句时间片
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param words 单字时间片集合
     */
    record SentenceTimeSpan(
            @JsonProperty("begin_time") int begin,
            @JsonProperty("end_time") int end,
            @JsonProperty("words") List<WordTimeSpan> words
    ) {

    }

}
