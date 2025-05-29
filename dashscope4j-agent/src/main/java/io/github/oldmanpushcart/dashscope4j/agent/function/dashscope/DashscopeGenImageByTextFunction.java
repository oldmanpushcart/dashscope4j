package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import lombok.Builder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@ChatFnName("dashscope_gen_image_by_text")
@ChatFnDescription("根据文本提示生成图片")
@Builder(builderMethodName = "newBuilder")
public class DashscopeGenImageByTextFunction implements ChatFunction<DashscopeGenImageByTextFunction.Parameter, DashscopeGenImageByTextFunction.Result> {

    @Builder.Default
    private Task.WaitStrategy waitStrategy = Task.WaitStrategies.until(
            Duration.ofSeconds(5),
            Duration.ofMinutes(1)
    );

    @Builder.Default
    private UnaryOperator<GenImageRequest> requestTransformer = t -> t;

    @Override
    public CompletionStage<Result> call(Tool.Caller caller, Parameter parameter) {

        final var request = GenImageRequest.newBuilder()
                .copyContextFrom(caller.request())
                .model(GenImageModel.WANX_V2_1_PLUS)
                .option(GenImageOptions.NUMBER, 1)
                .prompt(parameter.prompt())
                .building(builder -> ofNullable(parameter.negative()).ifPresent(builder::negative))
                .build();

        final var newRequest = requestTransformer.apply(request);

        return caller.client().image().generation().task(newRequest)
                .thenCompose(this::waitingFor)
                .thenApply(this::responseToImageURIs)
                .thenApply(Result::new);
    }

    private <T> CompletionStage<T> waitingFor(Task.Half<T> half) {
        return half.waitingFor(waitStrategy);
    }

    private List<URI> responseToImageURIs(GenImageResponse response) {
        return response.output().results().stream()
                .filter(GenImageResponse.Item::isSuccess)
                .map(GenImageResponse.Item::image)
                .collect(Collectors.toList());
    }


    public record Parameter(

            @JsonPropertyDescription("正向提示，描述期望图像包含的内容")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("负向提示，描述不期望图像包含的内容")
            @JsonProperty
            String negative

    ) {

    }


    public record Result(
            @JsonPropertyDescription("生成图像的URI列表")
            @JsonProperty
            List<URI> imageURIs
    ) {

    }

}
