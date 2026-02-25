package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

public class FileDetailRequest extends OpenAiRequest<FileDetailResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String identity;

    public FileDetailRequest(String identity) {
        super(FileDetailResponse.class);
        this.identity = identity;
    }


    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/detail/ >>> identnty={}", identity);
        return HttpRequest.newBuilder()
                .uri(EndpointUtils.https(host ,"/compatible-mode/v1/files/" + identity))
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileDetailResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/detail <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileDetailResponse.class, this, httpResponse);
        });
    }

}
