package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;

import static java.util.Objects.isNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class FileDetailResponse extends OpenAiResponse<FileMeta> {

    FileMeta output;

    @JsonCreator
    private FileDetailResponse(

            @JacksonInject("header/x-request-id")
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
        super(uuid, error);
        this.output = isNull(error)
                ? new FileMeta(identity, name, size, Instant.ofEpochSecond(created), purpose)
                : null;
    }

}
