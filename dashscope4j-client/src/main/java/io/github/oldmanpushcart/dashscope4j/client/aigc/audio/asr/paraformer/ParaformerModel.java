package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan.Sentence;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.INFERENCE_PATH;

public record ParaformerModel(String name, String path)
        implements Model<ParaformerModel.In, ParaformerModel.Out> {

    public static final ParaformerModel PARAFORMER_REALTIME_V1 = new ParaformerModel("paraformer-realtime-v1", INFERENCE_PATH);
    public static final ParaformerModel PARAFORMER_REALTIME_V2 = new ParaformerModel("paraformer-realtime-v2", INFERENCE_PATH);

    public static final ParaformerModel PARAFORMER_REALTIME_8K_V1 = new ParaformerModel("paraformer-realtime-8k-v1", INFERENCE_PATH);
    public static final ParaformerModel PARAFORMER_REALTIME_8K_V2 = new ParaformerModel("paraformer-realtime-8k-v2", INFERENCE_PATH);


    public static class In {
        private In() {

        }
    }

    public record Out(
            @JsonProperty("output")
            Output output
    ) {

        public record Output(
                @JsonProperty("sentence")
                Sentence sentence
        ) {

        }

    }

}
