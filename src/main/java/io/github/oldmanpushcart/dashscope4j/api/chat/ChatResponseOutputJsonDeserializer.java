package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse.Choice;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.MessageCodec;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

class ChatResponseOutputJsonDeserializer extends JsonDeserializer<ChatResponse.Output> {

    private static final OutputDeserializer[] deserializers = new OutputDeserializer[]{
            new TextFormatOutputDeserializer(),
            new MessageFormatOutputDeserializer()
    };

    @Override
    public ChatResponse.Output deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        final JsonNode node = context.readTree(parser);
        for (final OutputDeserializer deserializer : deserializers) {
            final ChatResponse.Output output = deserializer.deserialize(context, node);
            if (Objects.nonNull(output)) {
                return output;
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface OutputDeserializer {

        ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException;

    }

    private static class MessageFormatOutputDeserializer implements OutputDeserializer {

        @Override
        public ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException {

            final JsonNode searchNode = node.get("search_info");
            final ChatResponse.SearchInfo search = context.readTreeAsValue(searchNode, ChatResponse.SearchInfo.class);

            final JsonNode choicesNode = node.get("choices");

            // 如果没有 choices 节点，说明不是 message
            if (Objects.isNull(choicesNode)) {
                return null;
            }

            final List<Choice> choices = new ArrayList<>();
            for (final JsonNode choiceNode : choicesNode) {

                // 结束原因
                final JsonNode finishNode = choiceNode.required("finish_reason");
                final ChatResponse.Finish finish = context.readTreeAsValue(finishNode, ChatResponse.Finish.class);

                // 单消息
                if (choiceNode.has("message")) {
                    final JsonNode messageNode = choiceNode.required("message");
                    final Message message = MessageCodec.decode(messageNode);
                    choices.add(new Choice(finish, message));
                }

                // 多消息：见于plugin场景
                else if (choiceNode.has("messages")) {
                    final JsonNode messagesNode = choiceNode.required("messages");
                    final List<Message> messages = new ArrayList<>();
                    for (final JsonNode messageNode : messagesNode) {
                        final Message message = MessageCodec.decode(messageNode);
                        messages.add(message);
                    }
                    choices.add(new Choice(finish, unmodifiableList(messages)));
                }

            }

            // 返回应答数据
            return new ChatResponse.Output(search, unmodifiableList(choices));
        }

    }

    private static class TextFormatOutputDeserializer implements OutputDeserializer {

        @Override
        public ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException {

            final JsonNode searchNode = node.get("search_info");
            final ChatResponse.SearchInfo search = context.readTreeAsValue(searchNode, ChatResponse.SearchInfo.class);

            final JsonNode choicesNode = node.get("choices");

            // 如果有 choices 节点，说明不是 text only
            if (Objects.nonNull(choicesNode)) {
                return null;
            }

            final InnerOutput data = context.readTreeAsValue(node, InnerOutput.class);
            final Choice choice = new Choice(data.finish, Message.ofAi(data.text));
            return new ChatResponse.Output(search, choice);
        }

        @Value
        @Accessors(fluent = true)
        @Builder(access = AccessLevel.PRIVATE)
        @Jacksonized
        private static class InnerOutput {

            @JsonProperty("finish_reason")
            ChatResponse.Finish finish;

            @JsonProperty
            String text;

        }

    }
}
