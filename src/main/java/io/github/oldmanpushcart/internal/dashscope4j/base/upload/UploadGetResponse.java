package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.time.Duration;

/**
 * 上传凭证获取响应
 */
public record UploadGetResponse(String uuid, Ret ret, Usage usage, Output output)
        implements ApiResponse<UploadGetResponse.Output> {

    public record Output(Upload upload) implements ApiResponse.Output {

        private static final long MB_TO_BYTE = 1024L * 1024L;

        @JsonCreator
        static Output of(
                @JsonProperty("policy") String policy,
                @JsonProperty("signature") String signature,
                @JsonProperty("upload_dir") String directory,
                @JsonProperty("upload_host") String host,
                @JsonProperty("expire_in_seconds") int expireInSeconds,
                @JsonProperty("max_file_size_mb") int maxFileSizeMb,
                @JsonProperty("capacity_limit_mb") long capacityLimitMb,
                @JsonProperty("oss_access_key_id") String ossAccessKeyId,
                @JsonProperty("x_oss_object_acl") String xOssObjectAcl,
                @JsonProperty("x_oss_forbid_overwrite") boolean xOssForbidOverwrite
        ) {
            return new Output(
                    new Upload(
                            policy,
                            signature,
                            Duration.ofSeconds(expireInSeconds),
                            maxFileSizeMb * MB_TO_BYTE,
                            capacityLimitMb * MB_TO_BYTE,
                            new Upload.Oss(
                                    host, directory,
                                    ossAccessKeyId,
                                    xOssObjectAcl,
                                    xOssForbidOverwrite
                            )
                    )
            );
        }

    }


    @JsonCreator
    static UploadGetResponse of(
            @JsonProperty("request_id") String uuid,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("data") Output output
    ) {
        return new UploadGetResponse(uuid, Ret.of(code, message), Usage.empty(), output);
    }

}
