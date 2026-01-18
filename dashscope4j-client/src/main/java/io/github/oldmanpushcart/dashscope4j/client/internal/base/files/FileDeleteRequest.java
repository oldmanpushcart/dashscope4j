package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

public class FileDeleteRequest extends OpenAiRequest<FileDeleteResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String identity;

    public FileDeleteRequest(String identity) {
        super(FileDeleteResponse.class);
        this.identity = identity;
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/delete >>> identity={}", identity);
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host ,"/compatible-mode/v1/files/" + identity))
                .DELETE()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileDeleteResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/delete <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileDeleteResponse.class, this, httpResponse);
        });
    }

}
