package io.github.oldmanpushcart.dashscope4j.internal.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@Value
@Accessors(fluent = true)
@Jacksonized
@Builder
public class OpenAiError {

    @JsonProperty("code")
    String code;

    @JsonProperty("message")
    String message;

    @JsonProperty("type")
    String type;

    @JsonProperty("param")
    String param;

}
