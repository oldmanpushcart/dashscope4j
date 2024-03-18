package io.github.oldmanpushcart.test.dashscope4j.chat.function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.chat.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.function.ChatFunction;

import java.util.concurrent.CompletableFuture;

@ChatFn(name = "echo", description = "echo words")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

    @Override
    public CompletableFuture<Echo> call(Echo echo) {
        return CompletableFuture.completedFuture(new Echo(echo.words()));
    }

    public record Echo(
            @JsonPropertyDescription("words")
            String words
    ) {

    }

}
