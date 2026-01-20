package io.github.oldmanpushcart.dashscope4j.client.vision.t2v;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageResponse;

import java.net.URI;

public class Text2VideoResponse extends AlgoResponse<Text2VideoResponse.Output> {

    private final Output output;

    @JsonCreator
    private Text2VideoResponse(

            @JacksonInject("dashscope/request")
            Text2VideoRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(request, uuid, code, desc, usage);
        this.output = output;
    }

    @Override
    public Output output() {
        return output;
    }

    public static class Output {

        private final URI video;
        private final String originalPrompt;
        private final String actualPrompt;

        @JsonCreator
        private Output(

                @JsonProperty("video_url")
                URI video,

                @JsonProperty("orig_prompt")
                String originalPrompt,

                @JsonProperty("actual_prompt")
                String actualPrompt

        ) {
            this.video = video;
            this.originalPrompt = originalPrompt;
            this.actualPrompt = actualPrompt;
        }

        public URI video() {
            return video;
        }

        public String originalPrompt() {
            return originalPrompt;
        }

        public String actualPrompt() {
            return actualPrompt;
        }

    }

}
