package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiResponse;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FileListResponse extends OpenAiResponse<List<FileMeta>> {

    List<FileMeta> output;

    private FileListResponse(String uuid, OpenAiError error, List<FileMeta> output) {
        super(uuid, error);
        this.output = output;
    }

    @JsonCreator
    private static FileListResponse of(

            @JacksonInject("header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("data")
            List<Data> list

    ) {
        final List<FileMeta> metas = list.stream()
                .map(Data::toMeta)
                .collect(Collectors.toList());
        return new FileListResponse(uuid, error, Collections.unmodifiableList(metas));
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder
    private static class Data {

        @JsonProperty("id")
        String id;

        @JsonProperty("object")
        String object;

        @JsonProperty("bytes")
        Long bytes;

        @JsonProperty("created_at")
        Integer created;

        @JsonProperty("filename")
        String filename;

        @JsonProperty("purpose")
        Purpose purpose;

        @JsonProperty("status")
        String status;

        FileMeta toMeta() {
            return new FileMeta(
                    id,
                    filename,
                    bytes,
                    Instant.ofEpochSecond(created),
                    purpose
            );
        }

    }

}
