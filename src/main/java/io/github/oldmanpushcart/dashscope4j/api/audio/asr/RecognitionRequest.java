package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class RecognitionRequest extends ApiRequest<RecognitionModel, RecognitionResponse> {

    @JsonProperty("task_group")
    private final String group = "audio";

    @JsonProperty("task")
    private final String task = "asr";

    @JsonProperty("function")
    private final String fn = "recognition";

    private RecognitionRequest(Builder builder) {
        super(RecognitionResponse.class, builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(RecognitionRequest request) {
        return new Builder(request);
    }

    public static class Builder extends ApiRequest.Builder<RecognitionModel, RecognitionRequest, Builder> {

        public Builder() {

        }

        public Builder(RecognitionRequest request) {
            super(request);
        }

        @Override
        public RecognitionRequest build() {
            return new RecognitionRequest(this);
        }

    }

}
