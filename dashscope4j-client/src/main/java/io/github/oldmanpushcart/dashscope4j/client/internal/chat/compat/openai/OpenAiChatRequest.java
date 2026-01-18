package io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
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

    public OpenAiChatRequest(ChatRequest chatRequest) {
        super(OpenAiChatResponse.class);
        this.ref = chatRequest;
        this.model = chatRequest.model();
        this.parameters = chatRequest.parameters();
        this.messages = chatRequest.messages();
        this.tools = chatRequest.tools();
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

}
