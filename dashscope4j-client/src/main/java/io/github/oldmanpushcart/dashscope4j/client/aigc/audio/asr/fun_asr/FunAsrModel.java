package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.timespan.Sentence;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.INFERENCE_PATH;

public record FunAsrModel(String name, String path) implements Model<FunAsrModel.In, FunAsrModel.Out> {

    public static final FunAsrModel FUN_ASR_REALTIME = new FunAsrModel("fun-asr-realtime", INFERENCE_PATH);

    public static class In {
        private In() {

        }
    }

    public record Out(
            @JsonProperty("output")
            Out.Output output
    ) {

        public record Output(
                @JsonProperty("sentence")
                Sentence sentence
        ) {

        }

    }

}
