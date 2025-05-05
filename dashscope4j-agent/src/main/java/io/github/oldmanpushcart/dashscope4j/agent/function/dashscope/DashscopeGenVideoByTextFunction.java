package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoModel;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_gen_video_by_text")
@ChatFnDescription("根据文本提示生成视频")
@Setter
@Accessors(fluent = true, chain = true)
public class DashscopeGenVideoByTextFunction implements ChatFunction<DashscopeGenVideoByTextFunction.Parameter, DashscopeGenVideoByTextFunction.Result> {

    private Task.WaitStrategy waitStrategy = Task.WaitStrategies.until(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5)
    );

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final TextGenVideoRequest request = TextGenVideoRequest.newBuilder()
                .model(TextGenVideoModel.WANX_V2_1_T2V_TURBO)
                .option(TextGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                .prompt(parameter.prompt())
                .build();

        return caller.client().video().genByText()
                .task(request)
                .thenCompose(this::waitingFor)
                .thenApply(response -> response.output().video())
                .thenApply(Result::new);
    }

    private <T> CompletionStage<T> waitingFor(Task.Half<T> half) {
        return half.waitingFor(waitStrategy);
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder(builderMethodName = "newBuilder")
    public static class Parameter {

        @JsonPropertyDescription("描述生成视频所期待的内容")
        @JsonProperty(required = true)
        String prompt;

    }

    @Value
    @Accessors(fluent = true)
    public static class Result {

        @JsonPropertyDescription("生成视频的URI")
        @JsonProperty
        URI videoURI;

    }

}
