/**
 * FunASR 语音识别模型。
 */
package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.timespan.Sentence;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.INFERENCE_PATH;

/**
 * FunASR 语音识别模型。
 */
public record FunAsrModel(String name, String path) implements Model<FunAsrModel.In, FunAsrModel.Out> {

    /**
     * FunASR 实时语音识别模型实例。
     */
    public static final FunAsrModel FUN_ASR_REALTIME = new FunAsrModel("fun-asr-realtime", INFERENCE_PATH);

    /**
     * 模型输入定义。
     */
    public static class In {
        private In() {

        }
    }

    /**
     * 模型输出定义。
     */
    public record Out(
            @JsonProperty("output")
            Output output
    ) {

        /**
         * 输出内容。
         */
        public record Output(
                @JsonProperty("sentence")
                Sentence sentence
        ) {

        }

    }

}
