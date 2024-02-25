package io.github.ompc.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.chat.message.Message;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

record ChatResponseImpl(String uuid, Ret ret, Usage usage, Output output, Choice best) implements ChatResponse {

    @JsonCreator
    static ChatResponseImpl of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            OutputImpl output
    ) {

        // 获取最好的选择
        final var best = Optional.ofNullable(output)
                .map(Output::choices)
                .flatMap(choices -> choices.stream().sorted().findFirst())
                .orElse(null);

        return new ChatResponseImpl(uuid, Ret.of(code, message), usage, output, best);
    }

    @JsonDeserialize(using = OutputImpl.DataJsonDeserializer.class)
    public record OutputImpl(List<Choice> choices) implements ChatResponse.Output {

        static class DataJsonDeserializer extends JsonDeserializer<Output> {

            @Override
            public Output deserialize(JsonParser parser, DeserializationContext context) throws IOException {

                final var node = context.readTree(parser);

                // openai格式
                if (node.has("choices")) {
                    final var choiceArray = context.readTreeAsValue(node.get("choices"), ChoiceImpl[].class);
                    return new OutputImpl(List.of(choiceArray));
                }

                // 老格式
                final var finish = context.readTreeAsValue(node.get("finish_reason"), Finish.class);
                final var message = Message.ofAi(node.get("text").asText());
                return new OutputImpl(List.of(new ChoiceImpl(finish, message)));

            }

        }

    }

    public record ChoiceImpl(Finish finish, Message message) implements Choice {

        @JsonCreator
        static ChoiceImpl of(
                @JsonProperty("finish_reason")
                Finish finish,
                @JsonProperty("message")
                Message message
        ) {
            return new ChoiceImpl(finish, message);
        }

    }

}
