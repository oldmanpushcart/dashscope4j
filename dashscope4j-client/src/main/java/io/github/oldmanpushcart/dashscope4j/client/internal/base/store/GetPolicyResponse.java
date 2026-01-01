package io.github.oldmanpushcart.dashscope4j.client.internal.base.store;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.time.Duration;
import java.time.Instant;

public class GetPolicyResponse extends ApiResponse {

    private static final long MB_TO_BYTE = 1024L * 1024L;
    private final Output output;

    @JsonCreator
    public GetPolicyResponse(

            @JacksonInject("dashscope/request")
            GetPolicyRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("data")
            Output output

    ) {
        super(request, uuid, code, desc);
        this.output = output;
    }

    public Output output() {
        return output;
    }

    public record Output(Policy policy) {

        @JsonCreator
        public Output(

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
            this(new Policy(
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
            ));
        }

    }

}
