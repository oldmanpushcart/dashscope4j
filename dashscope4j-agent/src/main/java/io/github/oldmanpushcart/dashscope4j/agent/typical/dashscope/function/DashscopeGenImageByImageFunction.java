package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.function;

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
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@ChatFnName("dashscope_gen_image_by_image")
@ChatFnDescription("根据参考图片和文本提示生成图片")
@Setter
@Accessors(fluent = true, chain = true)
public class DashscopeGenImageByImageFunction
        implements ChatFunction<DashscopeGenImageByImageFunction.Parameter, DashscopeGenImageByImageFunction.Result> {

    private boolean autoUploadEnabled;
    private Task.WaitStrategy waitStrategy = Task.WaitStrategies.until(
            Duration.ofSeconds(5),
            Duration.ofMinutes(1)
    );

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final GenImageRequest request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .enableAutoUpload(autoUploadEnabled)
                .option(GenImageOptions.NUMBER, 1)
                .prompt(parameter.prompt())
                .reference(parameter.referenceImage())
                .building(builder -> {
                    if (null != parameter.negative()) {
                        builder.negative(parameter.negative());
                    }
                    if (null != parameter.refMode()) {
                        builder.option(GenImageOptions.REF_MODE, parameter.refMode());
                    }
                    if (null != parameter.refStrength()) {
                        builder.option(GenImageOptions.REF_STRENGTH, parameter.refStrength());
                    }
                })
                .build();

        return caller.client().image().generation().task(request)
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

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder
    public static class Parameter {

        @JsonPropertyDescription("参考图像的URI")
        @JsonProperty(required = true)
        URI referenceImage;

        @JsonPropertyDescription("正向提示，描述期望图像包含的内容")
        @JsonProperty(required = true)
        String prompt;

        @JsonPropertyDescription("负向提示，描述不期望图像包含的内容")
        @JsonProperty
        String negative;

        @JsonPropertyDescription("参考图像的匹配模式")
        @JsonProperty
        GenImageOptions.RefMode refMode;

        @JsonPropertyDescription("参考图像的匹配强度。" +
                                 "取值范围为[0.0, 1.0]。取值越大，代表生成的图像与参考图越相似。")
        @JsonProperty
        Float refStrength;

    }

    @Value
    public static class Result {

        @JsonPropertyDescription("生成图像的URI列表")
        @JsonProperty
        List<URI> imageURIs;

    }

}
