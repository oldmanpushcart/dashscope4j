package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.Error;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;

import java.time.Instant;

public record FileDetailResponse(Error error, Output output)
        implements OpenAiResponse<FileDetailResponse.Output> {

    public record Output(FileMeta meta) implements OpenAiResponse.Output {
    }

    @JsonCreator
    static FileDetailResponse of(
            @JsonProperty("error") Error error,
            @JsonProperty("id") String id,
            @JsonProperty("filename") String name,
            @JsonProperty("bytes") Long size,
            @JsonProperty("created_at") Integer created,
            @JsonProperty("purpose") String purpose
    ) {
        return null != error ?
                new FileDetailResponse(error, null) :
                new FileDetailResponse(null, new Output(
                        new FileMetaImpl(
                                id,
                                name,
                                size,
                                Instant.ofEpochSecond(created).toEpochMilli(),
                                purpose
                        )));
    }

}
