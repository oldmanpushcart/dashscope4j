package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.OpenAiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class FileDeleteResponse extends OpenAiResponse<Boolean> {

    Boolean output;

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
        this.output = Boolean.TRUE.equals(deleted);
    }

}
