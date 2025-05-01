package io.github.oldmanpushcart.dashscope4j.agent.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_understanding_for_visual")
@ChatFnDescription("可根据提示词要求图片、视频进行理解")
public class DashscopeUnderstandingForVisualFunction
        implements ChatFunction<DashscopeUnderstandingForVisualFunction.Parameter, DashscopeUnderstandingForVisualFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
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

    @Value
    @Accessors(fluent = true)
    public static class Parameter {

        @JsonPropertyDescription("提示词")
        @JsonProperty(required = true)
        String prompt;

        @JsonPropertyDescription("图片资源URI列表")
        @JsonProperty(required = true)
        List<URI> imageURIs;

        @JsonPropertyDescription("视频资源URI列表")
        @JsonProperty(required = true)
        List<URI> videoURIs;

    }

    @Value
    public static class Result {

        @JsonPropertyDescription("识别结果")
        @JsonProperty
        CharSequence text;

    }

}
