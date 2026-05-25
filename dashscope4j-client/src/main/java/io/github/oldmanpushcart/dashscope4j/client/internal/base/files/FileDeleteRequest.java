package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

public class FileDeleteRequest extends OpenAiRequest<FileDeleteResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String identity;

    public FileDeleteRequest(String identity) {
        super(FileDeleteResponse.class);
        this.identity = identity;
    }

    @Override
    public Request toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/delete >>> identity={}", identity);
        final var endpoint = EndpointUtils.https(host, "/compatible-mode/v1/files/" + identity);
        return new Request.Builder()
                .url(endpoint.toString())
                .delete()
                .build();
    }

    @Override
    public BiFunction<Response, String, FileDeleteResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/delete <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileDeleteResponse.class, this, httpResponse);
        });
    }

}
