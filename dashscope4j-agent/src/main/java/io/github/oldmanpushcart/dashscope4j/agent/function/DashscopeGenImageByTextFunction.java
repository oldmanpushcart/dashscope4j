package io.github.oldmanpushcart.dashscope4j.agent.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@ChatFnName("dashscope_text2image")
@ChatFnDescription("根据文本提示生成图片")
public class DashscopeGenImageByTextFunction implements ChatFunction<DashscopeGenImageByTextFunction.Parameter, DashscopeGenImageByTextFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V2_1_PLUS)
                .option(GenImageOptions.NUMBER, 1)
                .prompt(parameter.prompt())
                .building(builder -> {
                    if (null != parameter.negative()) {
                        builder.negative(parameter.negative());
                    }
                })
                .build();

        return caller.client().image().generation().task(request)
                .thenCompose(this::waitingFor)
                .thenApply(this::responseToImageURIs)
                .thenApply(Result::new);
    }

    private <T> CompletionStage<T> waitingFor(Task.Half<T> half) {
        return half.waitingFor(Task.WaitStrategies.until(
                Duration.ofSeconds(5),
                Duration.ofMinutes(1)
        ));
    }

    private List<URI> responseToImageURIs(GenImageResponse response) {
        return response.output().results().stream()
                .filter(GenImageResponse.Item::isSuccess)
                .map(GenImageResponse.Item::image)
                .collect(Collectors.toList());
    }

    @Value
    @Accessors(fluent = true)
    public static class Parameter {

        @JsonPropertyDescription("正向提示，描述期望图像包含的内容")
        @JsonProperty(required = true)
        String prompt;

        @JsonPropertyDescription("负向提示，描述不期望图像包含的内容")
        @JsonProperty
        String negative;

    }

    @Value
    public static class Result {

        @JsonPropertyDescription("生成图像的URI列表")
        @JsonProperty
        List<URI> imageURIs;

    }

}
