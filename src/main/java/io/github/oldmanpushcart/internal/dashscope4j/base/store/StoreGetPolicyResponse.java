package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

import java.time.Duration;

/**
 * 获取凭证响应
 */
public record StoreGetPolicyResponse(
        String uuid,
        Ret ret,
        Usage usage,
        Output output
) implements HttpApiResponse<StoreGetPolicyResponse.Output> {

    public record Output(StorePolicy policy) {

        private static final long MB_TO_BYTE = 1024L * 1024L;

        @JsonCreator
        static Output of(

                @JsonProperty("policy")
                String value,

                @JsonProperty("signature")
                String signature,

                @JsonProperty("upload_dir")
                String directory,

                @JsonProperty("upload_host")
                String host,

                @JsonProperty("expire_in_seconds")
                int expireInSeconds,

                @JsonProperty("max_file_size_mb")
                int maxFileSizeMb,

                @JsonProperty("capacity_limit_mb")
                long capacityLimitMb,

                @JsonProperty("oss_access_key_id")
                String ossAccessKeyId,

                @JsonProperty("x_oss_object_acl")
                String xOssObjectAcl,

                @JsonProperty("x_oss_forbid_overwrite")
                boolean xOssForbidOverwrite

        ) {

            return new Output(
                    new StorePolicy(
                            value,
                            signature,
                            Duration.ofSeconds(expireInSeconds),
                            maxFileSizeMb * MB_TO_BYTE,
                            capacityLimitMb * MB_TO_BYTE,
                            new StorePolicy.Oss(
                                    host,
                                    directory,
                                    ossAccessKeyId,
                                    xOssObjectAcl,
                                    xOssForbidOverwrite
                            )
                    ));
        }

    }


    @JsonCreator
    static StoreGetPolicyResponse of(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Output output

    ) {
        return new StoreGetPolicyResponse(uuid, Ret.of(code, message), Usage.empty(), output);
    }

}
