package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiResponse;

public class FileDeleteResponse extends OpenAiResponse {

    private final Boolean deleted;

    @JsonCreator
    private FileDeleteResponse(

            @JacksonInject("dashscope/request")
            FileDeleteRequest request,

            @JacksonInject("http/header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("deleted")
            Boolean deleted

    ) {
        super(request, uuid, error);
        this.deleted = Boolean.TRUE.equals(deleted);
        ;
    }

    public Boolean deleted() {
        return deleted;
    }

}
