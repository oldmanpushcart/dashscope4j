package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiResponse;

import java.time.Instant;

public class FileDetailResponse extends OpenAiResponse {

    private final FileMeta meta;

    @JsonCreator
    protected FileDetailResponse(

            @JacksonInject("dashscope/request")
            FileDetailRequest request,

            @JacksonInject("http/header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("id")
            String identity,

            @JsonProperty("filename")
            String name,

            @JsonProperty("bytes")
            long size,

            @JsonProperty("created_at")
            int created,

            @JsonProperty("purpose")
            Purpose purpose

    ) {
        super(request, uuid, error);
        this.meta = null == error
                ? new FileMeta(identity, name, size, Instant.ofEpochSecond(created), purpose)
                : null;
    }

    public FileMeta meta() {
        return meta;
    }

}
