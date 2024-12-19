package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.Getter;
import lombok.experimental.Accessors;
import okhttp3.Request;

/**
 * 语音识别请求
 */
@Getter
@Accessors(fluent = true)
public final class RecognitionRequest extends AlgoRequest<RecognitionModel, RecognitionResponse> {

    @JsonProperty("task_group")
    private final String group = "audio";

    @JsonProperty("task")
    private final String task = "asr";

    @JsonProperty("function")
    private final String fn = "recognition";

    private RecognitionRequest(Builder builder) {
        super(RecognitionResponse.class, builder);
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

    public static Builder newBuilder(RecognitionRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<RecognitionModel, RecognitionRequest, Builder> {

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
