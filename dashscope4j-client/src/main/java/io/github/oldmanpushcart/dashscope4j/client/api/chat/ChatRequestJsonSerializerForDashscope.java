package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.PluginMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ChatRequestJsonSerializerForDashscope extends JsonSerializer<ChatRequest> {

    @Override
    public void serialize(ChatRequest request, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        serializeModel(request.model(), generator);
        serializeParameters(request.parameters(), generator);
        serializeInput(request, generator);
        generator.writeEndObject();
    }

    private void serializeModel(ChatModel model, JsonGenerator generator) throws IOException {
        generator.writeStringField("model", model.name());
    }

    private void serializeParameters(Parameters parameters, JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("parameters");
        for (final Map.Entry<String, Object> entry : parameters.dump().entrySet()) {
            final var name = entry.getKey();
            final var value = entry.getValue();
            if (null != value) {
                generator.writeObjectField(name, value);
            }
        }
        generator.writeEndObject();
    }

    private void serializeInput(ChatRequest request, JsonGenerator generator) throws IOException {
        generator.writeObjectFieldStart("input");
        serializeInputMessages(request, request.messages(), generator);
        generator.writeEndObject();
    }

    private void serializeInputMessages(ChatRequest request, List<Message> messages, JsonGenerator generator) throws IOException {
        generator.writeArrayFieldStart("messages");
        for (final Message message : messages) {
            serializeInputMessage(request, message, generator);
        }
        generator.writeEndArray();
    }

    private void serializeInputMessage(ChatRequest request, Message message, JsonGenerator generator) throws IOException {
        final var mode = request.model().mode();
        generator.writeStartObject();

        generator.writeObjectField("role", message.role());
        generator.writeStringField("reasoning_content", message.reasoningContent());

        if (ChatModel.Mode.TEXT == mode) {
            generator.writeStringField("content", message.text());
        } else if (ChatModel.Mode.MULTIMODAL == mode) {
            generator.writeArrayFieldStart("content");
            for (final var content : message.contents()) {
                generator.writePOJO(Map.of(content.type(), content.data()));
            }
            generator.writeEndArray();
        } else {
            throw new IllegalArgumentException("unsupported mode: " + mode);
        }

        if(message instanceof PluginMessage) {

        }

        generator.writeEndObject();
    }

}
