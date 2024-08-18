package io.github.oldmanpushcart.dashscope4j.audio.tts;

import io.github.oldmanpushcart.dashscope4j.audio.tts.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoResponse;

/**
 * 语音合成应答
 *
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

}
