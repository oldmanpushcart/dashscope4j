package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.timespan.Transcription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.timespan.Translation;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.util.List;

public record GummyModel(String name, String path) implements Model<GummyModel.In, GummyModel.Out> {

    public static final GummyModel GUMMY_CHAT_V1 = new GummyModel("gummy-chat-v1", Constants.INFERENCE_PATH);
    public static final GummyModel GUMMY_REALTIME_V1 = new GummyModel("gummy-realtime-v1", Constants.INFERENCE_PATH);

    public static class In {
        private In() {

        }
    }

    public record Out(

            @JsonProperty("output")
            Output output

    ) {

        public record Output(

                @JsonProperty("translations")
                List<Translation> translations,

                @JsonProperty("transcription")
                Transcription transcription

        ) {

        }

    }

}
