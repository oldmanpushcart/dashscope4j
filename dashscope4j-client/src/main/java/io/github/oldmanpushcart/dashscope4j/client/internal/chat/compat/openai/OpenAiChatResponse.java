package io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse.Finish;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message.Role;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.DurationMsJsonSerializer;

import java.time.Duration;
import java.util.List;

public class OpenAiChatResponse extends OpenAiResponse {

    private final List<Choice> choices;
    private final Usage usage;

    @JsonCreator
    public OpenAiChatResponse(

            @JacksonInject("dashscope/request")
            OpenAiChatRequest request,

            @JsonProperty("id")
            String uuid,

            @JsonProperty("choices")
            List<Choice> choices,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("error")
            OpenAiError error

    ) {
        super(request, uuid, error);
        this.choices = choices;
        this.usage = null == usage ? Usage.empty() : usage;
    }

    public OpenAiChatRequest request() {
        return (OpenAiChatRequest) super.request();
    }

    public List<Choice> choices() {
        return choices;
    }

    public Usage usage() {
        return usage;
    }

    public record Choice(

            @JsonProperty("finish_reason")
            Finish finish,

            @JsonProperty("message")
            @JsonAlias("delta")
            Message message,

            @JsonProperty("model")
            String model,

            @JsonProperty("usage")
            Usage usage

    ) {


    }

    public record Message(

            @JsonProperty("role")
            Role role,

            @JsonProperty("content")
            String content,

            @JsonProperty("reasoning_content")
            String reasoningContent,

            @JsonProperty("audio")
            Audio audio,

            @JsonProperty("tool_calls")
            List<Tool.Call> calls

    ) {

    }

    public record Audio(

            @JsonProperty("data")
            String data,

            @JsonProperty("expires_at")
            @JsonSerialize(using = DurationMsJsonSerializer.class)
            Duration expiresAt

    ) {

    }

}
