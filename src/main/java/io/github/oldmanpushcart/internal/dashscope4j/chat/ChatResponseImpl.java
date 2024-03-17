package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;

record ChatResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements ChatResponse {

    @JsonCreator
    static ChatResponseImpl of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            OutputImpl output
    ) {
        return new ChatResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

}
