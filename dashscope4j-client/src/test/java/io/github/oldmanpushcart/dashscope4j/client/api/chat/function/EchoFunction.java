package io.github.oldmanpushcart.dashscope4j.client.api.chat.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ChatFnName("echo")
@ChatFnDescription("当用户输入echo:，回显后边的文字")
public class EchoFunction implements ChatFunction<EchoFunction.Echo, EchoFunction.Echo> {

    @Override
    public CompletionStage<Echo> call(Tool.Caller caller, Echo echo) {
        return CompletableFuture.completedStage(echo);
    }

    public record Echo(

            @JsonProperty(value = "text")
            @JsonPropertyDescription("需要回显的文字")
            String text

    ) {

    }

}
