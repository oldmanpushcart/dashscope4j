package io.github.oldmanpushcart.test.dashscope4j.chat.function;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("the result of function call")
public record Result<T>(
        @JsonPropertyDescription("success or not")
        @JsonProperty("success")
        boolean isOk,

        @JsonPropertyDescription("message")
        String message,

        @JsonPropertyDescription("data")
        T data
) {

}