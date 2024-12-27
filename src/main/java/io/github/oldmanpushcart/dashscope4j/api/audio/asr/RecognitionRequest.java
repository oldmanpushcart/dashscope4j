package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import okhttp3.Request;

/**
 * 语音识别请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RecognitionRequest extends AlgoRequest<RecognitionModel, RecognitionResponse> {

    @JsonProperty("task_group")
    String group = "audio";

    @JsonProperty("task")
    String task = "asr";

    @JsonProperty("function")
    String fn = "recognition";

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
