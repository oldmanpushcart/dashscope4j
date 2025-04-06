package io.github.oldmanpushcart.dashscope4j.api.audio.tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import okhttp3.Request;

import static java.util.Objects.requireNonNull;

/**
 * 语音合成请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SpeechSynthesisRequest extends AlgoRequest<SpeechSynthesisModel, SpeechSynthesisResponse> {

    @JsonProperty("task_group")
    String group = "audio";

    @JsonProperty("task")
    String task = "tts";

    @JsonProperty("function")
    String fn = "SpeechSynthesizer";

    /**
     * 合成文本
     */
    String text;

    private SpeechSynthesisRequest(Builder builder) {
        super(SpeechSynthesisResponse.class, builder);
        this.text = builder.text;
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
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
            this.text = request.text();
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
