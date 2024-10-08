package io.github.oldmanpushcart.internal.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.Error;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;

public record FileDeleteResponse(Error error, Output output)
        implements OpenAiResponse<FileDeleteResponse.Output> {

    public record Output(Boolean deleted) {

    }

    @JsonCreator
    public static FileDeleteResponse of(
            @JsonProperty("error") Error error,
            @JsonProperty("deleted") Boolean deleted
    ) {
        return null != error ?
                new FileDeleteResponse(error, null) :
                new FileDeleteResponse(null, new Output(deleted));
    }

}
