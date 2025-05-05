package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

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
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_understanding_document")
@ChatFnDescription("可根据提示词要求对文档进行理解")
@Setter
@Accessors(fluent = true, chain = true)
public class DashscopeUnderstandingDocumentFunction
        implements ChatFunction<DashscopeUnderstandingDocumentFunction.Parameter, DashscopeUnderstandingDocumentFunction.Result> {

    private boolean autoUpload;

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .enableAutoUpload(autoUpload)
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

        if (null != parameter.documentURIs()) {
            parameter.documentURIs().stream()
                    .map(Content::ofFile)
                    .forEach(contents::add);
        }

        return Message.ofUser(contents);
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder(builderMethodName = "newBuilder")
    public static class Parameter {

        @JsonPropertyDescription("提示词")
        @JsonProperty(required = true)
        String prompt;

        @JsonPropertyDescription("文档URI列表")
        @JsonProperty(required = true)
        List<URI> documentURIs;

    }

    @Value
    @Accessors(fluent = true)
    public static class Result {

        @JsonPropertyDescription("识别结果")
        @JsonProperty
        CharSequence text;

    }

}