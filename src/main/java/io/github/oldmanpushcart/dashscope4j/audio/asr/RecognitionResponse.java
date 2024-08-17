package io.github.oldmanpushcart.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoResponse;

import java.util.List;

/**
 * 音频识别应答
 *
 * @since 2.2.0
 */
public interface RecognitionResponse extends ExchangeAlgoResponse<RecognitionResponse.Output> {

    /**
     * 应答数据
     */
    interface Output {

        /**
         * @return 句子时间片
         */
        SentenceTimeSpan sentence();

    }

    /**
     * 单字时间片
     *
     * @param begin       开始偏移量(ms)
     * @param end         结束偏移量(ms)
     * @param text        单字文本
     * @param punctuation 标点符号
     */
    record WordTimeSpan(
            @JsonProperty("begin_time") int begin,
            @JsonProperty("end_time") int end,
            @JsonProperty("text") String text,
            @JsonProperty("punctuation") String punctuation
    ) {

    }

    /**
     * 整句时间片
     *
     * @param begin 开始偏移量(ms)
     * @param end   结束偏移量(ms)
     * @param text  句子内容
     * @param words 单字时间片集合
     */
    record SentenceTimeSpan(
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

}
