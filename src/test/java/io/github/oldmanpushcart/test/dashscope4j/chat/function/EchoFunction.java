package io.github.oldmanpushcart.test.dashscope4j.chat.function;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;

import java.util.concurrent.CompletableFuture;

@ChatFn(name = "echo", description = "当用户输入echo:，回显后边的文字")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

    @Override
    public CompletableFuture<Echo> call(Echo echo) {
        return CompletableFuture.completedFuture(new Echo(echo.words()));
    }

    public record Echo(
            @JsonPropertyDescription("需要回显的文字")
            String words
    ) {

    }

}
