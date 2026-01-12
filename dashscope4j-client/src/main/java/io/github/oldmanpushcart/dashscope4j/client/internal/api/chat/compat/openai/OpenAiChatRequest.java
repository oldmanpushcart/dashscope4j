package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_CONTENT_TYPE;
import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

@JsonSerialize(using = OpenAiChatRequestJsonSerializer.class)
public class OpenAiChatRequest extends OpenAiRequest<OpenAiChatResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChatModel model;
    private final Parameters parameters;
    private final List<Message> messages;
    private final List<Tool> tools;
    private final ChatRequest ref;

    protected OpenAiChatRequest(Builder builder) {
        super(OpenAiChatResponse.class, builder);
        this.ref = builder.ref;
        this.model = builder.model;
        this.parameters = builder.parameters;
        this.messages = builder.messages;
        this.tools = builder.tools;
    }

    public ChatRequest ref() {
        return ref;
    }

    public ChatModel model() {
        return model;
    }

    public Parameters parameters() {
        return parameters;
    }

    public List<Message> messages() {
        return messages;
    }

    public List<Tool> tools() {
        return tools;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        final var endpoint = EndpointUtils.https(host, model.path());
        return HttpRequest.newBuilder(endpoint)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestEncoder().apply(this)))
                .build();
    }

    protected Function<ApiRequest<?>, String> requestEncoder() {
        return request -> {
            final var requestBody = JacksonJsonUtils.toJson(this);
            logger.debug("dashscope4j-client://algo/openai/{} >>> {}", model.name(), requestBody);
            return requestBody;
        };
    }

    @Override
    public BiFunction<HttpResponse<?>, String, OpenAiChatResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://algo/openai/{} <<< {}", model.name(), responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, responseType(), this, httpResponse);
        };
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(OpenAiChatRequest request) {
        return new Builder(request);
    }

    public static class Builder extends OpenAiRequest.Builder<OpenAiChatRequest, Builder> {

        private ChatRequest ref;
        private ChatModel model;
        private final Parameters parameters = new Parameters();
        private final List<Message> messages = new ArrayList<>();
        private final List<Tool> tools = new ArrayList<>();


        public Builder() {
            super();
        }

        public Builder(OpenAiChatRequest request) {
            super(request);
            this.ref = request.ref;
            this.model = request.model;
            this.parameters.merge(request.parameters);
            this.messages.addAll(request.messages);
            this.tools.addAll(request.tools);
        }

        public Builder ref(ChatRequest ref) {
            requireNonNull(ref);
            this.ref = ref;
            return this;
        }

        public Builder model(ChatModel model) {
            requireNonNull(model);
            this.model = model;
            return this;
        }

        public Builder parameters(Parameters parameters) {
            requireNonNull(parameters);
            this.parameters.merge(parameters);
            return this;
        }

        public <PT, PR> Builder parameter(Parameters.ParameterKey<PT, PR> parameterKey, PT value) {
            requireNonNull(parameterKey);
            parameters.append(parameterKey, value);
            return self();
        }

        public Builder parameter(String name, Object value) {
            requireNonBlankString(name, "Parameter name must not be blank");
            parameters.append(name, value);
            return self();
        }

        public Builder messages(Collection<? extends Message> messages) {
            requireNonNull(messages);
            this.messages.clear();
            this.messages.addAll(messages);
            return this;
        }

        public Builder addMessages(Collection<? extends Message> messages) {
            requireNonNull(messages);
            this.messages.addAll(messages);
            return this;
        }

        public Builder addMessage(Message message) {
            requireNonNull(message);
            this.messages.add(message);
            return this;
        }

        public Builder tools(Collection<? extends Tool> tools) {
            requireNonNull(tools);
            this.tools.clear();
            this.tools.addAll(tools);
            return this;
        }

        public Builder addTools(Collection<? extends Tool> tools) {
            requireNonNull(tools);
            this.tools.addAll(tools);
            return this;
        }

        public Builder addTool(Tool tool) {
            requireNonNull(tool);
            this.tools.add(tool);
            return this;
        }

        @Override
        public OpenAiChatRequest build() {
            return new OpenAiChatRequest(this);
        }

    }

}
