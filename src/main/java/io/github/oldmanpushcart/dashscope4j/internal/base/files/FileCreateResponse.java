package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Objects;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FileCreateResponse extends OpenAiResponse<FileMeta> {

    FileMeta output;

    private FileCreateResponse(String uuid, OpenAiError error, FileMeta output) {
        super(uuid, error);
        this.output = output;
    }

    @JsonCreator
    private static FileCreateResponse of(

            @JacksonInject("header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("id")
            String identity,

            @JsonProperty("filename")
            String name,

            @JsonProperty("bytes")
            Long size,

            @JsonProperty("created_at")
            Integer created,

            @JsonProperty("purpose")
            String purpose

    ) {

        if (Objects.nonNull(error)) {
            return new FileCreateResponse(uuid, error, null);
        }

        final FileMeta meta = new FileMeta(
                identity,
                name,
                size,
                Instant.ofEpochSecond(created),
                purpose
        );
        return new FileCreateResponse(uuid, null, meta);

    }

}
