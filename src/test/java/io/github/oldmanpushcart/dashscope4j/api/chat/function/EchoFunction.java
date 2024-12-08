package io.github.oldmanpushcart.dashscope4j.api.chat.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//@ChatFnName("echo")
@ChatFnDescription("当用户输入echo:，回显后边的文字")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

    @Override
    public CompletionStage<Echo> call(Echo echo) {
        return CompletableFuture.completedFuture(new Echo(echo.words()));
    }

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class Echo {

        @JsonProperty
        @JsonPropertyDescription("需要回显的文字")
        String words;

    }

}
