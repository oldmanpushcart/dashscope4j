package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.util.function.BiFunction;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class FileListRequest extends OpenAiRequest<FileListResponse> {

    private FileListRequest(Builder builder) {
        super(FileListResponse.class, builder);
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/files/list >>> {}", "ALL");
        return new Request.Builder()
                .url("https://dashscope.aliyuncs.com/compatible-mode/v1/files")
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, FileListResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/files/list <<< {}", bodyJson);
            return JacksonJsonUtils.toObject(bodyJson, FileListResponse.class, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileListRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<FileListRequest, Builder> {

        public Builder() {

        }

        public Builder(FileListRequest request) {
            super(request);
        }

        @Override
        public FileListRequest build() {
            return new FileListRequest(this);
        }

    }

}
