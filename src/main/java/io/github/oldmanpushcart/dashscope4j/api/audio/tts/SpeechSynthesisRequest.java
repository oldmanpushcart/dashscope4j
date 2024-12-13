package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.Request;

import java.util.HashMap;

import static java.util.Objects.requireNonNull;

@Getter
@Accessors(fluent = true)
public final class SpeechSynthesisRequest extends AlgoRequest<SpeechSynthesisModel, SpeechSynthesisResponse> {

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
            put("text", text);
        }};
    }

    @Override
    public Request newHttpRequest() {
        return new Request.Builder()
                .url(model().remote().toString())
                .get()
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(SpeechSynthesisRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<SpeechSynthesisModel, SpeechSynthesisRequest, Builder> {

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
