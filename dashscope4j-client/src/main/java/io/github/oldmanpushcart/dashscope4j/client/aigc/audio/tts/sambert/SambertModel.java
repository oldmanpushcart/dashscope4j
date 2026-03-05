package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

import java.util.Collections;
import java.util.Map;

import static io.github.oldmanpushcart.dashscope4j.client.Constants.INFERENCE_PATH;

public record SambertModel(
        String name,
        String path,
        Map<String, Object> parameters
) implements Model<SambertModel.In, SambertModel.Out> {

    public static final SambertModel ZHINAN = new SambertModel(
            "sambert-zhinan-v1",
            Map.of("sample_rate", 48000)
    );

    public SambertModel(String name, Map<String, Object> parameters) {
        this(name, INFERENCE_PATH, parameters);
    }

    public SambertModel(String name, String path, Map<String, Object> parameters) {
        this.name = name;
        this.path = path;
        this.parameters = Collections.unmodifiableMap(parameters);
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
