package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ChatRequestJsonSerializer extends JsonSerializer<ChatRequest> {

    @Override
    public void serialize(ChatRequest request, JsonGenerator generator, SerializerProvider provider) throws IOException {
        final var view = decideViews(request);
        if(ChatView.Dashscope.class.isAssignableFrom(view)) {
            serializeDashscope(request, view, generator, provider);
        } else if(ChatView.OpenAI.class.isAssignableFrom(view)) {
            serializeOpenAI(request, view, generator, provider);
        } else {
            throw new IllegalArgumentException("Unsupported view: " + view);
        }
    }

    private Class<?> decideViews(ChatRequest request) {
        return switch (request.compatibility()) {
            case DASHSCOPE -> switch (request.model().mode()) {
                case TEXT -> ChatView.DashscopeText.class;
                case MULTIMODAL -> ChatView.DashscopeMultimodal.class;
            };
            case OPENAI -> switch (request.model().mode()){
                case TEXT -> ChatView.OpenAIText.class;
                case MULTIMODAL -> ChatView.OpenAIMultimodal.class;
            };
        };
    }

    private void serializeDashscope(ChatRequest request, Class<?> view, JsonGenerator generator, SerializerProvider provider) throws IOException {

        final var pojo = new HashMap<>();
        pojo.put("model", request.model());

        final var newParameters = new Parameters()
                .merge(request.parameters());
        final List<Tool> enabledTools = request.tools().stream()
                .filter(Tool::isEnabled)
                .collect(Collectors.toList());
        if (!enabledTools.isEmpty()) {
            newParameters.append("result_format", "message");
            newParameters.append("tools", enabledTools);
        }
        pojo.put("parameters", newParameters);

        final var input = new HashMap<>();
        input.put("messages", request.messages());
        pojo.put("input", input);

        final var mapper = (ObjectMapper) generator.getCodec();
        mapper.writerWithView(view).writeValue(generator, pojo);

    }

    private void serializeOpenAI(ChatRequest request, Class<?> view, JsonGenerator generator, SerializerProvider provider) throws IOException {
        final var pojo = new HashMap<>();
        pojo.put("model", request.model());
        pojo.put("messages", request.messages());

        final var newParameters = new Parameters()
                .merge(request.parameters());
        final List<Tool> enabledTools = request.tools().stream()
                .filter(Tool::isEnabled)
                .collect(Collectors.toList());
        if (!enabledTools.isEmpty()) {
            newParameters.append("tools", enabledTools);
        }
        newParameters.forEach(pojo::put);

        final var mapper = (ObjectMapper) generator.getCodec();
        mapper.writerWithView(view).writeValue(generator, pojo);
    }

}
