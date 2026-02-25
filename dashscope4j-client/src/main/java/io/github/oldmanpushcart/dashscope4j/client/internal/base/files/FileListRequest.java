package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import io.github.oldmanpushcart.dashscope4j.client.internal.OpenAiRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.BiFunction;

public class FileListRequest extends OpenAiRequest<FileListResponse> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String after;
    private final int limit;

    public FileListRequest(String after, int limit) {
        super(FileListResponse.class);
        this.after = after;
        this.limit = limit;
    }

    private URI genQueryURI(String host) {
        final StringBuilder builder = new StringBuilder("/compatible-mode/v1/files?1=1");
        if (limit > 0) {
            builder.append(String.format("&limit=%s", limit));
        }
        if (Objects.nonNull(after)) {
            builder.append(String.format("&after=%s", after));
        }
        return EndpointUtils.https(host, "/compatible-mode/v1/files?1=1" + builder);
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        logger.debug("dashscope4j-client://base/files/list >>> after={};limit={};", after, limit);
        return HttpRequest.newBuilder()
                .uri(genQueryURI(host))
                .GET()
                .build();
    }

    @Override
    public BiFunction<HttpResponse<?>, String, FileListResponse> responseDecoder() {
        return ((httpResponse, responseBody) -> {
            logger.debug("dashscope4j-client://base/files/list <<< {}", responseBody);
            return JacksonJsonUtils.toApiResponse(responseBody, FileListResponse.class, this, httpResponse);
        });
    }

}
