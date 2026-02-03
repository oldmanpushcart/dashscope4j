package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.timespan.Sentence;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;

public record CosyVoiceModel(
        String name,
        String path
) implements Model<CosyVoiceModel.In, CosyVoiceModel.Out> {

    private static final String PATH = "/api-ws/v1/inference/";

    public static final CosyVoiceModel COSYVOICE_V1 = new CosyVoiceModel("cosyvoice-v1");
    public static final CosyVoiceModel COSYVOICE_V2 = new CosyVoiceModel("cosyvoice-v2");
    public static final CosyVoiceModel COSYVOICE_V3_PLUS = new CosyVoiceModel("cosyvoice-v3-plus");
    public static final CosyVoiceModel COSYVOICE_V3_FLASH = new CosyVoiceModel("cosyvoice-v3-flash");

    public CosyVoiceModel(String name) {
        this(name, PATH);
    }

    /**
     * 模型输入
     */
    public record In(

            @JsonProperty("input")
            Input input

    ) {

        public record Input(

                @JsonProperty("text")
                String text

        ) {

        }

        public static In of(String text) {
            return new In(new Input(text));
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

                @JsonProperty("type")
                Type type,

                @JsonProperty("original_text")
                String text,

                @JsonProperty("sentence")
                Sentence sentence

        ) {
        }

        public enum Type {

            @JsonProperty("sentence-begin")
            SENTENCE_BEGIN,

            @JsonProperty("sentence-synthesis")
            SENTENCE_SYNTHESIS,

            @JsonProperty("sentence-end")
            SENTENCE_END

        }

    }

}
