package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;

import static io.github.oldmanpushcart.internal.dashscope4j.util.StringUtils.isNotBlank;
import static java.util.Objects.requireNonNull;

@Getter
@Accessors(fluent = true)
public final class SpeechSynthesisRequest extends ApiRequest<SpeechSynthesisModel, SpeechSynthesisResponse> {

    @JsonProperty("task_group")
    private final String group = "audio";

    @JsonProperty("task")
    private final String task = "tts";

    @JsonProperty("function")
    private final String fn = "SpeechSynthesizer";

    private final String text;

    private SpeechSynthesisRequest(Builder builder) {
        super(SpeechSynthesisResponse.class, builder);
        this.text = builder.text;
    }

    @Override
    protected Object input() {
        return new HashMap<Object, Object>() {{

            if (isNotBlank(text)) {
                put("text", text);
            }

        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(SpeechSynthesisRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<SpeechSynthesisModel, SpeechSynthesisRequest, Builder> {

        private String text;

        public Builder() {

        }

        public Builder(SpeechSynthesisRequest request) {
            super(request);
        }

        public Builder text(String text) {
            this.text = requireNonNull(text);
            return this;
        }

        @Override
        public SpeechSynthesisRequest build() {
            return new SpeechSynthesisRequest(this);
        }

    }

}
