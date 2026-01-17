package io.github.oldmanpushcart.dashscope4j.client.internal.image.t2i.compat.chat;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.client.Usage;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;

import java.util.List;

public class Text2ImageChatResponse extends AlgoResponse<Text2ImageChatResponse.Output> {

    private final Output output;

    @JsonCreator
    protected Text2ImageChatResponse(

            @JacksonInject("dashscope/request")
            Text2ImageChatRequest request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(request, uuid, code, desc, usage);
        this.output = output;
    }

    @Override
    public Output output() {
        return output;
    }

    public record Output(

            @JsonProperty("choices")
            List<ChatResponse.Choice> choices
    ) {

    }

}
