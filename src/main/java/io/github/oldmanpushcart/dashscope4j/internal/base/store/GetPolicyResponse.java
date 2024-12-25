package io.github.oldmanpushcart.dashscope4j.internal.base.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class GetPolicyResponse extends ApiResponse<GetPolicyResponse.Output> {

    Output output;

    @JsonCreator
    private GetPolicyResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("data")
            Output output

    ) {
        super(uuid, code, message);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Output {

        private static final long MB_TO_BYTE = 1024L * 1024L;
        Policy policy;

        @JsonCreator
        private Output(

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
            this.policy = new Policy(
                    value,
                    signature,
                    Instant.now().plus(Duration.ofSeconds(expireInSeconds)),
                    maxFileSizeMb * MB_TO_BYTE,
                    capacityLimitMb * MB_TO_BYTE,
                    new Policy.Oss(
                            host,
                            directory,
                            ossAccessKeyId,
                            xOssObjectAcl,
                            xOssForbidOverwrite
                    )
            );

        }
    }

}
