package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toUnmodifiableList;

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
    public CompletionStage<Result> call(Tool.Caller caller, Parameter parameter) {
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
    record Parameter(

            @JsonPropertyDescription("执行的任务内容")
            @JsonProperty(required = true)
            String prompt,

            @JsonPropertyDescription("任务执行所需要的文本信息")
            @JsonProperty()
            List<Part.Text> texts,

            @JsonPropertyDescription("任务执行所需要的多模态信息")
            @JsonProperty()
            List<Part.Media> medias

    ) {

        private Message toMessage() {

            final List<Content<?>> contents = Stream.of(List.of(new Part.Text(prompt)), texts, medias)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(part ->
                            switch (part.type()) {
                                case TEXT -> Content.ofText((String) part.data());
                                case IMAGE -> Content.ofImage((URI) part.data());
                                case VIDEO -> Content.ofVideo((URI) part.data());
                                case AUDIO -> Content.ofAudio((URI) part.data());
                                case FILE -> Content.ofFile((URI) part.data());
                            })
                    .collect(toUnmodifiableList());

            return Message.ofUser(contents);
        }

    }

    /**
     * 信息
     */
    sealed interface Part<T> permits Part.Text, Part.Media {

        @JsonPropertyDescription("类型")
        @JsonProperty(value = "type", required = true)
        Content.Type type();

        T data();

        record Text(
                @JsonPropertyDescription("文本数据")
                @JsonProperty(value = "data", required = true)
                String data
        ) implements Part<String> {

            @Override
            public Content.Type type() {
                return Content.Type.TEXT;
            }

        }

        record Media(

                Content.Type type,

                @JsonPropertyDescription("""
                        URI数据
                        - 必须严格符合URI格式：scheme://username:password@hostname:port/path?query#fragment
                        - 可接受本地文件URI格式：file://hose/path
                        """
                )
                @JsonProperty(value = "data", required = true)
                URI data

        ) implements Part<URI> {

        }

    }

    /**
     * 函数结果
     */
    record Result(
            @JsonPropertyDescription("返回结果")
            @JsonProperty
            String output
    ) {

    }

}
