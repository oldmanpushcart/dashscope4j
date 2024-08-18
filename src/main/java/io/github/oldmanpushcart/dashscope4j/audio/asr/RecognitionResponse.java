package io.github.oldmanpushcart.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoResponse;

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

}
