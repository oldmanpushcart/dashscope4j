package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse.Choice;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatResponseOutputJsonDeserializer extends JsonDeserializer<ChatResponse.Output> {

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
            final JsonNode choicesNode = node.get("choices");

            // 如果没有 choices 节点，说明不是 message
            if (Objects.isNull(choicesNode)) {
                return null;
            }

            final List<Choice> choices = new ArrayList<>();
            for (final JsonNode choiceNode : choicesNode) {

                // 结束原因
                final ChatResponse.Finish finish = context.readTreeAsValue(choiceNode.get("finish_reason"), ChatResponse.Finish.class);

                // 单消息
                if (choiceNode.has("message")) {

                    final JsonNode messageNode = choiceNode.get("message");
                    final JsonNode contentNode = messageNode.get("content");

                    // 处理多模态内容
                    if (contentNode.isArray()) {
                        final InnerMultiMessage inMultiMessage = context.readTreeAsValue(messageNode, InnerMultiMessage.class);
                        final Message message = new Message(inMultiMessage.role, inMultiMessage.contents());
                        choices.add(new Choice(finish, message));
                    }

                    // 处理文本内容
                    else {
                        final InnerTextMessage inTextMessage = context.readTreeAsValue(messageNode, InnerTextMessage.class);
                        final Message message = deserializeMessage(context, messageNode, inTextMessage);
                        choices.add(new Choice(finish, message));
                    }

                }

                // 多消息：见于plugin场景
                else if (choiceNode.has("messages")) {
                    final JsonNode messagesNode = choiceNode.get("messages");
                    final List<Message> messages = new ArrayList<>();
                    for (final JsonNode messageNode : messagesNode) {
                        final InnerTextMessage inTextMessage = context.readTreeAsValue(messageNode, InnerTextMessage.class);
                        final Message message = deserializeMessage(context, messageNode, inTextMessage);
                        messages.add(message);
                    }
                    choices.add(new Choice(finish, messages));
                }

            }

            // 返回应答数据
            return new ChatResponse.Output(choices);
        }

        private Message deserializeMessage(DeserializationContext context, JsonNode messageNode, InnerTextMessage inTextMessage) throws IOException {

            // 处理插件应答消息
            if (inTextMessage.role() == Message.Role.PLUGIN) {
                final String text = inTextMessage.text();
                final String name = messageNode.get("name").asText();
                final Plugin.Status status = context.readTreeAsValue(messageNode.get("status"), Plugin.Status.class);
                return new PluginMessage(text, name, status);
            }

            // 处理插件请求消息
            else if (inTextMessage.role() == Message.Role.AI && messageNode.has("plugin_call")) {
                final String text = inTextMessage.text();
                final Plugin.Call call = context.readTreeAsValue(messageNode.get("plugin_call"), Plugin.Call.class);
                return new PluginCallMessage(text, call);
            }

            // 处理工具应答消息
            else if (inTextMessage.role() == Message.Role.TOOL) {
                final String text = inTextMessage.text();
                final String name = messageNode.get("name").asText();
                return new ToolMessage(text, name);
            }

            // 处理工具请求消息
            else if (inTextMessage.role() == Message.Role.AI && messageNode.has("tool_calls")) {
                final String text = inTextMessage.text();
                final JsonNode toolCallsNode = messageNode.get("tool_calls");
                final List<Tool.Call> toolCalls = new ArrayList<>();
                for (final JsonNode toolCallNode : toolCallsNode) {
                    final Tool.Classify type = context.readTreeAsValue(toolCallNode.get("type"), Tool.Classify.class);
                    if (type == Tool.Classify.FUNCTION) {
                        final ChatFunctionTool.Call call = context.readTreeAsValue(toolCallNode.get("function"), ChatFunctionTool.Call.class);
                        toolCalls.add(call);
                    }
                }
                return new ToolCallMessage(text, toolCalls);
            }

            // 处理普通消息
            else {
                final Message.Role role = inTextMessage.role();
                final String text = inTextMessage.text();
                return new Message(role, Content.ofText(text));
            }

        }

        @Value
        @Accessors(fluent = true)
        @AllArgsConstructor
        @Builder(access = AccessLevel.PRIVATE)
        @Jacksonized
        private static class InnerMultiMessage {

            @JsonProperty
            Message.Role role;

            @JsonProperty("content")
            List<Content<?>> contents;

        }

        @Value
        @Accessors(fluent = true)
        @AllArgsConstructor
        @Builder(access = AccessLevel.PRIVATE)
        @Jacksonized
        private static class InnerTextMessage {

            @JsonProperty
            Message.Role role;

            @JsonProperty("content")
            String text;

        }

    }

    private static class TextFormatOutputDeserializer implements OutputDeserializer {

        @Override
        public ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException {
            final JsonNode choicesNode = node.get("choices");

            // 如果有 choices 节点，说明不是 text only
            if (Objects.nonNull(choicesNode)) {
                return null;
            }

            final InnerOutput data = context.readTreeAsValue(node, InnerOutput.class);
            final Choice choice = new Choice(data.finish, Message.ofAi(data.text));
            return new ChatResponse.Output(choice);
        }

        @Value
        @Accessors(fluent = true)
        @AllArgsConstructor
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
