package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.INFERENCE_PATH;

public record SambertModel(
        String name,
        String path,
        Parameters parameters
) implements Model<SambertModel.In, SambertModel.Out> {

    public static final SambertModel ZHINAN = new SambertModel(
            "sambert-zhinan-v1",
            new Parameters()
                    .append(SambertParameterKeys.SAMPLE_RATE, 48000)
    );

    public SambertModel(String name, Parameters parameters) {
        this(name, INFERENCE_PATH, parameters);
    }

    public SambertModel(String name, String path, Parameters parameters) {
        this.name = name;
        this.path = path;
        this.parameters = parameters.unmodifiable();
    }


    /**
     * 模型输入
     */
    public static class In {

        private In() {

        }

    }

    /**
     * 模型输出
     *
     * @param output 输出结果
     * @param usage  使用情况
     */
    public record Out(

            @JsonProperty("output")
            Output output,

            @JsonProperty("usage")
            Usage usage

    ) {

        public record Output(

                @JsonProperty("sentence")
                SentenceTimeSpan sentence

        ) {

        }

    }


}
