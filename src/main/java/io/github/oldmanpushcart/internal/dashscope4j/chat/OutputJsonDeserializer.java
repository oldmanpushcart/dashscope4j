package io.github.oldmanpushcart.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.*;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.FunctionTool;
import io.github.oldmanpushcart.internal.dashscope4j.chat.tool.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OutputJsonDeserializer extends JsonDeserializer<ChatResponse.Output> {

    @Override
    public ChatResponse.Output deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        final var node = context.readTree(parser);
        for (final var deserializer : deserializers) {
            final var output = deserializer.deserialize(context, node);
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

    private static final OutputDeserializer textOutput = new OutputDeserializer() {

        @Override
        public ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException {
            final var choicesNode = node.get("choices");

            // 如果有 choices 节点，说明不是 text only
            if (Objects.nonNull(choicesNode)) {
                return null;
            }

            final var data = context.readTreeAsValue(node, InnerOutput.class);
            final var choice = new ChoiceImpl(data.finish, Message.ofAi(data.text));
            return new OutputImpl(choice);
        }

        private record InnerOutput(
                @JsonProperty("finish_reason")
                ChatResponse.Finish finish,
                @JsonProperty("text")
                String text
        ) {

        }

    };

    private static final OutputDeserializer messageOutput = new OutputDeserializer() {

        @Override
        public ChatResponse.Output deserialize(DeserializationContext context, JsonNode node) throws IOException {
            final var choicesNode = node.get("choices");

            // 如果没有 choices 节点，说明不是 message
            if (Objects.isNull(choicesNode)) {
                return null;
            }

            final var choices = new ArrayList<ChatResponse.Choice>();
            for (final var choiceNode : choicesNode) {

                // 结束原因
                final var finish = context.readTreeAsValue(choiceNode.get("finish_reason"), ChatResponse.Finish.class);

                // 单消息
                if (choiceNode.has("message")) {

                    final var messageNode = choiceNode.get("message");
                    final var contentNode = messageNode.get("content");

                    // 处理多模态内容
                    if (contentNode.isArray()) {
                        final var inMultiMessage = context.readTreeAsValue(messageNode, InnerMultiMessage.class);
                        final var message = new MessageImpl(inMultiMessage.role, new ArrayList<>(inMultiMessage.contents()));
                        choices.add(new ChoiceImpl(finish, message));
                    }

                    // 处理文本内容
                    else {
                        final var inTextMessage = context.readTreeAsValue(messageNode, InnerTextMessage.class);
                        final var message = deserializeMessage(context, messageNode, inTextMessage);
                        choices.add(new ChoiceImpl(finish, message));
                    }

                }

                // 多消息：见于plugin场景
                else if (choiceNode.has("messages")) {
                    final var messagesNode = choiceNode.get("messages");
                    final var messages = new ArrayList<Message>();
                    for (final var messageNode : messagesNode) {
                        final var inTextMessage = context.readTreeAsValue(messageNode, InnerTextMessage.class);
                        final var message = deserializeMessage(context, messageNode, inTextMessage);
                        messages.add(message);
                    }
                    choices.add(new ChoiceImpl(finish, messages));
                }

            }

            // 返回应答数据
            return new OutputImpl(choices);
        }

        private Message deserializeMessage(DeserializationContext context, JsonNode messageNode, InnerTextMessage inTextMessage) throws IOException {

            // 处理插件应答消息
            if (inTextMessage.role() == Message.Role.PLUGIN) {
                final var text = inTextMessage.text();
                final var name = messageNode.get("name").asText();
                final var status = context.readTreeAsValue(messageNode.get("status"), PluginMessageImpl.Status.class);
                return new PluginMessageImpl(text, name, status);
            }

            // 处理插件请求消息
            else if (inTextMessage.role() == Message.Role.AI && messageNode.has("plugin_call")) {
                final var text = inTextMessage.text();
                final var call = context.readTreeAsValue(messageNode.get("plugin_call"), PluginCallMessageImpl.Call.class);
                return new PluginCallMessageImpl(text, call);
            }

            // 处理工具应答消息
            else if (inTextMessage.role() == Message.Role.TOOL) {
                final var text = inTextMessage.text();
                final var name = messageNode.get("name").asText();
                return new ToolMessageImpl(text, name);
            }

            // 处理工具请求消息
            else if (inTextMessage.role() == Message.Role.AI && messageNode.has("tool_calls")) {
                final var text = inTextMessage.text();
                final var toolCallsNode = messageNode.get("tool_calls");
                final var toolCalls = new ArrayList<Tool.Call>();
                for (final var toolCallNode : toolCallsNode) {
                    final var type = context.readTreeAsValue(toolCallNode.get("type"), Tool.Classify.class);
                    if (type == Tool.Classify.FUNCTION) {
                        final var call = context.readTreeAsValue(toolCallNode.get("function"), FunctionTool.Call.class);
                        toolCalls.add(call);
                    }
                }
                return new ToolCallMessageImpl(text, toolCalls);
            }

            // 处理普通消息
            else {
                final var role = inTextMessage.role();
                final var text = inTextMessage.text();
                return new MessageImpl(role, List.of(Content.ofText(text)));
            }

        }

        private record InnerMultiMessage(
                @JsonProperty("role")
                Message.Role role,
                @JsonProperty("content")
                List<ContentImpl<?>> contents
        ) {

        }

        private record InnerTextMessage(
                @JsonProperty("role")
                Message.Role role,
                @JsonProperty("content")
                String text
        ) {

        }

    };

    private static final OutputDeserializer[] deserializers = new OutputDeserializer[]{
            textOutput,
            messageOutput
    };

}
