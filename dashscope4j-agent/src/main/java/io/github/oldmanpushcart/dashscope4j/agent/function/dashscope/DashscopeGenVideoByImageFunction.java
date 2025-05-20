package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoModel;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_gen_video_by_image")
@ChatFnDescription("根据参考图片和文本提示生成视频")
@Setter
@Accessors(fluent = true, chain = true)
public class DashscopeGenVideoByImageFunction
        implements ChatFunction<DashscopeGenVideoByImageFunction.Parameter, DashscopeGenVideoByImageFunction.Result> {

    private Task.WaitStrategy waitStrategy = Task.WaitStrategies.until(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5)
    );

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final ImageGenVideoRequest request = ImageGenVideoRequest.newBuilder()
                .model(ImageGenVideoModel.WANX_V2_1_I2V_TURBO)
                .context(AutoUploadContext.class, caller.request().context(AutoUploadContext.class))
                .option(ImageGenVideoOptions.ENABLE_PROMPT_EXTEND, true)
                .prompt(parameter.prompt())
                .image(parameter.referenceImage())
                .build();

        return caller.client().video().genByImage()
                .task(request)
                .thenCompose(this::waitingFor)
                .thenApply(response -> response.output().video())
                .thenApply(Result::new);

    }

    private <T> CompletionStage<T> waitingFor(Task.Half<T> half) {
        return half.waitingFor(waitStrategy);
    }


    public record Parameter(

            @JsonProperty(required = true)
            @JsonPropertyDescription("""
                    参考图像的URI
                    - 必须严格符合URI格式：scheme://username:password@hostname:port/path?query#fragment
                    - 可接受本地文件URI格式：file://hose/path
                    """
            )
            URI referenceImage,

            @JsonProperty(required = true)
            @JsonPropertyDescription("描述生成视频所期待的内容")
            String prompt

    ) {

    }

    public record Result(
            @JsonProperty
            @JsonPropertyDescription("生成视频的URI")
            URI videoURI
    ) {

    }

}
