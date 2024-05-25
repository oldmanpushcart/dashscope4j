package io.github.oldmanpushcart.internal.dashscope4j.base.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.Error;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;

public record FileCreateResponse(Error error, Output output)
        implements OpenAiResponse<FileCreateResponse.Output> {

    private FileCreateResponse(Error error) {
        this(error, null);
    }

    private FileCreateResponse(Output output) {
        this(null, output);
    }

    public record Output(FileMeta meta) implements OpenAiResponse.Output {
    }

    @JsonCreator
    static FileCreateResponse of(

            @JsonProperty("error")
            Error error,

            @JsonProperty("id")
            String id,

            @JsonProperty("filename")
            String name,

            @JsonProperty("bytes")
            Long size,

            @JsonProperty("created_at")
            Integer created,

            @JsonProperty("purpose")
            String purpose

    ) {

        return null != error ?
                new FileCreateResponse(error) :
                new FileCreateResponse(
                        new Output(
                                FileMetaImpl.of(
                                        id,
                                        name,
                                        size,
                                        created,
                                        purpose
                                )));

    }

}
