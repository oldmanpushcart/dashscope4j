package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiResponse;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class FileListResponse extends OpenAiResponse<List<FileMeta>> {

    List<FileMeta> output;
    boolean hasNext;

    @JsonCreator
    private FileListResponse(

            @JacksonInject("header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("data")
            List<Data> list,

            @JsonProperty("has_more")
            boolean hasNext

    ) {
        super(uuid, error);

        final List<FileMeta> metas = list.stream()
                .map(Data::toMeta)
                .collect(Collectors.toList());
        this.output = unmodifiableList(metas);
        this.hasNext = hasNext;
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    private static class Data {

        @JsonProperty("id")
        String id;

        @JsonProperty("object")
        String object;

        @JsonProperty("bytes")
        long bytes;

        @JsonProperty("created_at")
        int created;

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
