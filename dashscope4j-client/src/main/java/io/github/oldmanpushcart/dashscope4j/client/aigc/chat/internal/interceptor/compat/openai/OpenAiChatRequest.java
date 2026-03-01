package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.openai;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.MT_APPLICATION_JSON;

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

    public Map<String,Object> parameters() {
        return ref.parameters();
    }

    public List<Message> messages() {
        return ref.input().messages();
    }

    @Override
    public Request toHttpRequest(String host) {
        final var endpoint = EndpointUtils.https(host, model().path());
        final var requestBody = requestEncoder().apply(this);
        return new Request.Builder()
                .url(endpoint.toString())
                .post(RequestBody.create(requestBody, MT_APPLICATION_JSON))
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
    public BiFunction<Response, String, OpenAiChatResponse> responseDecoder() {
        return (httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://algo/openai/{} <<< {}", model().name(), responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, responseType(), this, httpResponse);
        };
    }

}
