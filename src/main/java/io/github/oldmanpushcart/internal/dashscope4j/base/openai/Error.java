package io.github.oldmanpushcart.internal.dashscope4j.base.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Error(

        @JsonProperty("message")
        String message,

        @JsonProperty("type")
        String type,

        @JsonProperty("param")
        String param,

        @JsonProperty("code")
        String code

) {
}
