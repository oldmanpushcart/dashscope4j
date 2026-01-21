package io.github.oldmanpushcart.dashscope4j.client.chat.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EchoFunction {

    public CompletionStage<Echo> call(Echo echo) {
        return CompletableFuture.completedStage(echo);
    }

    public record Echo(

            @JsonProperty
            @JsonPropertyDescription("需要回显的文字")
            String words

    ) {

    }

    public Tool toTool() {
        return FunctionTool.newBuilder()
                .name("echo")
                .description("当用户输入echo:，回显后边的文字")
                .function(this::call)
                .parameterType(Echo.class)
                .build();
    }

}
