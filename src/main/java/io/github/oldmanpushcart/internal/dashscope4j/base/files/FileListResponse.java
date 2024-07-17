package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.Error;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;

import java.time.Instant;
import java.util.List;

public record FileListResponse(Error error, Output output)
        implements OpenAiResponse<FileListResponse.Output> {

    public record Output(boolean hasMore, List<FileMeta> data) implements OpenAiResponse.Output {

    }

    @JsonCreator
    static FileListResponse of(
            @JsonProperty("error") Error error,
            @JsonProperty("has_more") Boolean hasMore,
            @JsonProperty("data") List<FileDetail> details
    ) {
        return null != error ?
                new FileListResponse(error, null) :
                new FileListResponse(null, new Output(
                        hasMore,
                        CollectionUtils.mapTo(details, FileDetail::toMeta)
                ));
    }

    private record FileDetail(
            @JsonProperty("id") String id,
            @JsonProperty("object") String object,
            @JsonProperty("bytes") Long bytes,
            @JsonProperty("created_at") Integer created,
            @JsonProperty("filename") String filename,
            @JsonProperty("purpose") String purpose,
            @JsonProperty("status") String status
    ) {
        FileMeta toMeta() {
            return new FileMetaImpl(
                    id,
                    filename,
                    bytes,
                    Instant.ofEpochSecond(created).toEpochMilli(),
                    purpose
            );
        }
    }

}
