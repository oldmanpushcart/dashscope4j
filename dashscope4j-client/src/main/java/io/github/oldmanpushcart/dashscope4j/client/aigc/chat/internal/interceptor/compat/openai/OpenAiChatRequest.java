package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.openai;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.HTTP_HEADER_CONTENT_TYPE;

@JsonSerialize(using = OpenAiChatRequestJsonSerializer.class)
class OpenAiChatRequest extends OpenAiRequest<OpenAiChatResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AigcRequest<ChatModel.Input, ?> ref;

    public OpenAiChatRequest(AigcRequest<ChatModel.Input, ?> ref) {
        super(OpenAiChatResponse.class);
        this.ref = ref;
    }

    public AigcRequest<ChatModel.Input, ?> ref() {
        return ref;
    }

    public AigcModel<?, ?> model() {
        return ref.model();
    }

    public Parameters parameters() {
        return ref.parameters();
    }

    public List<Message> messages() {
        return ref.input().messages();
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        final var endpoint = EndpointUtils.https(host, model().path());
        return HttpRequest.newBuilder(endpoint)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestEncoder().apply(this)))
                .build();
    }

    protected Function<ApiRequest<?>, String> requestEncoder() {
        return request -> {
            final var requestBody = JacksonJsonUtils.toJson(this);
            logger.debug("dashscope4j-client://algo/openai/{} >>> {}", model().name(), requestBody);
            return requestBody;
        };
    }

    @Override
    public BiFunction<HttpResponse<?>, String, OpenAiChatResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://algo/openai/{} <<< {}", model().name(), responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, responseType(), this, httpResponse);
        };
    }

}
