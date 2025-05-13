package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * 基础智能体函数
 */
class BaseChatAgentFunction
        implements ChatFunction<BaseChatAgentFunction.Parameter, BaseChatAgentFunction.Result> {

    private final BaseChatAgent agent;

    public BaseChatAgentFunction(BaseChatAgent agent) {
        this.agent = agent;
    }

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(caller.request().model())
                .copyContextFrom(caller.request())
                .addMessage(parameter.toMessage())
                .build();
        return agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(Result::new);
    }

    /**
     * 函数参数
     */
    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder
    public static class Parameter {

        @JsonPropertyDescription("描述希望执行的具体任务")
        @JsonProperty(required = true)
        String input;

        @JsonPropertyDescription("执行任务所必须的信息")
        @JsonProperty(required = true)
        List<Part> parts;

        @Value
        @Accessors(fluent = true)
        @Jacksonized
        @Builder
        public static class Part {

            @JsonPropertyDescription("类型")
            @JsonProperty(required = true)
            Type type;

            @JsonPropertyDescription("资源URI")
            @JsonProperty(required = true)
            URI uri;

            public enum Type {

                @JsonProperty("image")
                IMAGE,

                @JsonProperty("video")
                VIDEO,

                @JsonProperty("audio")
                AUDIO,

                @JsonProperty("file")
                FILE

            }

            /**
             * @return 转换为媒体内容
             */
            public Content<?> toContent() {
                switch (type) {
                    case IMAGE:
                        return Content.MediaContent.ofImage(uri);
                    case VIDEO:
                        return Content.MediaContent.ofVideo(uri);
                    case AUDIO:
                        return Content.MediaContent.ofAudio(uri);
                    case FILE:
                        return Content.MediaContent.ofFile(uri);
                    default:
                        throw new IllegalArgumentException("Unsupported type: " + type);
                }
            }

        }

        /**
         * @return 转换为消息
         */
        public Message toMessage() {

            final List<Content<?>> mediaContents = parts.stream()
                    .map(Part::toContent)
                    .collect(Collectors.toList());

            final List<Content<?>> contents = new ArrayList<>();
            contents.add(Content.ofText(input));
            contents.addAll(mediaContents);

            return Message.ofUser(contents);
        }

    }

    /**
     * 函数结果
     */
    @Data
    @Accessors(fluent = true, chain = true)
    public static class Result {

        @JsonPropertyDescription("返回结果")
        @JsonProperty
        String output;

        @JsonPropertyDescription("返回结果提示")
        @JsonProperty
        String prompt;

        public Result(String output) {
            this.output = output;
        }

    }

}
