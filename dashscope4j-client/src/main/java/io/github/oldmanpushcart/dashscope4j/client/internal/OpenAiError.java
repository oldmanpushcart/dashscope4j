package io.github.oldmanpushcart.dashscope4j.client.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenAiError(

        @JsonProperty("code")
        String code,

        @JsonProperty("message")
        String message,

        @JsonProperty("type")
        String type,

        @JsonProperty("param")
        String param

) {

}
