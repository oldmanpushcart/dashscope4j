package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.Error;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;

import java.util.List;

public record FileListResponse(Error error, Output output)
        implements OpenAiResponse<FileListResponse.Output> {

    private FileListResponse(Error error) {
        this(error, null);
    }

    private FileListResponse(Output output) {
        this(null, output);
    }

    public record Output(boolean hasMore, List<? extends FileMeta> data) implements OpenAiResponse.Output {

    }

    @JsonCreator
    static FileListResponse of(

            @JsonProperty("error")
            Error error,

            @JsonProperty("has_more")
            Boolean hasMore,

            @JsonProperty("data")
            List<FileMetaImpl> metas

    ) {

        return null != error ?
                new FileListResponse(error) :
                new FileListResponse(
                        new Output(
                                hasMore,
                                metas
                        ));

    }

}
