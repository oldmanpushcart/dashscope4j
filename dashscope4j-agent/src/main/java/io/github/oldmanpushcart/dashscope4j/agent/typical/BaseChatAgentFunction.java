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
import java.util.concurrent.CompletionStage;

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

            @JsonPropertyDescription("描述希望执行的具体任务")
            @JsonProperty(required = true)
            String input,

            @JsonPropertyDescription("""
                    执行任务所必须的URI
                    - 必须严格符合URI格式：scheme://username:password@hostname:port/path?query#fragment
                    - 可接受本地文件URI格式：file://hose/path
                    """
            )
            @JsonProperty(required = true)
            List<Resource> resources

    ) {


        /**
         * 资源
         *
         * @param type 类型
         * @param uri  地址
         */
        record Resource(

                @JsonPropertyDescription("类型")
                @JsonProperty(required = true)
                Type type,

                @JsonPropertyDescription("资源URI")
                @JsonProperty(required = true)
                URI uri

        ) {

            /**
             * 资源类型
             */
            public enum Type {
                @JsonProperty("image") IMAGE,
                @JsonProperty("video") VIDEO,
                @JsonProperty("audio") AUDIO,
                @JsonProperty("file") FILE
            }

            /**
             * @return 转换为媒体内容
             */
            public Content.MediaContent toMediaContent() {
                return switch (type) {
                    case IMAGE -> Content.MediaContent.ofImage(uri);
                    case VIDEO -> Content.MediaContent.ofVideo(uri);
                    case AUDIO -> Content.MediaContent.ofAudio(uri);
                    case FILE -> Content.MediaContent.ofFile(uri);
                };
            }

        }

        /**
         * @return 转换为消息
         */
        public Message toMessage() {
            final var mediaContents = resources.stream()
                    .map(Resource::toMediaContent)
                    .toList();
            return Message.ofUser(input, mediaContents);
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
