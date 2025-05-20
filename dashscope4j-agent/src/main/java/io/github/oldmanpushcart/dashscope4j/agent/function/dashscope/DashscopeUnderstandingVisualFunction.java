package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.AutoUploadContext;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_understanding_visual")
@ChatFnDescription("可根据提示词要求图片、视频进行理解")
@Setter
@Accessors(fluent = true, chain = true)
public class DashscopeUnderstandingVisualFunction
        implements ChatFunction<DashscopeUnderstandingVisualFunction.Parameter, DashscopeUnderstandingVisualFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .context(AutoUploadContext.class, caller.request().context(AutoUploadContext.class))
                .addMessage(newUserMessage(parameter))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        return caller.client().chat().directFlow(request)
                .map(response -> response.output().best().message().text())
                .reduce(new StringBuilder(), StringBuilder::append)
                .toCompletionStage()
                .thenApply(Result::new);

    }

    private Message newUserMessage(Parameter parameter) {

        final List<Content<?>> contents = new ArrayList<>();
        contents.add(Content.ofText(parameter.prompt()));

        if (null != parameter.imageURIs()) {
            parameter.imageURIs().stream()
                    .map(Content::ofImage)
                    .forEach(contents::add);
        }

        if (null != parameter.videoURIs()) {
            parameter.videoURIs().stream()
                    .map(Content::ofVideo)
                    .forEach(contents::add);
        }

        return Message.ofUser(contents);
    }


    public record Parameter(

            @JsonPropertyDescription("提示词")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("""
                    图片资源URI列表
                    - 必须严格符合URI格式：scheme://username:password@hostname:port/path?query#fragment
                    - 可接受本地文件URI格式：file://hose/path
                    - 不接受BASE64格式"""
            )
            @JsonProperty
            List<URI> imageURIs,

            @JsonPropertyDescription("""
                    视频资源URI列表
                    - 必须严格符合URI格式：scheme://username:password@hostname:port/path?query#fragment
                    - 可接受本地文件URI格式：file://hose/path
                    """
            )
            @JsonProperty
            List<URI> videoURIs

    ) {

    }


    public record Result(
            @JsonPropertyDescription("识别结果")
            @JsonProperty
            CharSequence text
    ) {

    }

}
