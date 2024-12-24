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

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class FileDetailRequest extends OpenAiRequest<FileDetailResponse> {

    String identity;

    private FileDetailRequest(Builder builder) {
        super(FileDetailResponse.class, builder);
        this.identity = builder.identity;
    }

    @Override
    public Request newHttpRequest() {
        log.debug("dashscope://base/files/detail >>> identity={}", identity);
        return new Request.Builder()
                .url(String.format("https://dashscope.aliyuncs.com/compatible-mode/v1/files/%s", identity))
                .get()
                .build();
    }

    @Override
    public BiFunction<Response, String, FileDetailResponse> newResponseDecoder() {
        return (httpResponse, bodyJson) -> {
            log.debug("dashscope://base/files/detail <<< {}", bodyJson);
            final FileDetailResponse response = JacksonJsonUtils.toObject(bodyJson, FileDetailResponse.class, httpResponse);

            /*
             * 如果查询文件详情失败且失败的原因是文件不存在，则返回null
             * output = null
             */
            return !response.isSuccess() && httpResponse.code() == 404
                    ? new FileDetailResponse(response.uuid(), null, null)
                    : response;
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(FileDetailRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<FileDetailRequest, Builder> {

        private String identity;

        public Builder() {

        }

        public Builder(FileDetailRequest request) {
            super(request);
        }

        public Builder identity(String identity) {
            this.identity = requireNonNull(identity);
            return this;
        }

        @Override
        public FileDetailRequest build() {
            requireNonNull(identity);
            return new FileDetailRequest(this);
        }
    }

}
