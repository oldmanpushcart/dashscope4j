package io.github.oldmanpushcart.dashscope4j.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.internal.base.OpenAiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FileDeleteResponse extends OpenAiResponse<Boolean> {

    Boolean output;

    FileDeleteResponse(String uuid, OpenAiError error, Boolean output) {
        super(uuid, error);
        this.output = output;
    }

    @JsonCreator
    public static FileDeleteResponse of(

            @JacksonInject("header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("deleted")
            Boolean deleted

    ) {
        return new FileDeleteResponse(uuid, error, Boolean.TRUE.equals(deleted));
    }

}
