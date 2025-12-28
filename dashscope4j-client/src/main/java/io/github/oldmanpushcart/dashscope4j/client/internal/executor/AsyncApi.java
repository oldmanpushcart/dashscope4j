package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class AsyncApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String ak;
    private final HttpClient http;

    public AsyncApi(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://async";
    }

    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(URI endpoint, T request) {

        final var requestBody = JacksonJsonUtils.toJson(request);
        logger.debug("{} >>> {}", this, requestBody);

        final var httpRequest = HttpRequest.newBuilder()
                .uri(endpoint)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    final var responseBody = httpResponse.body();
                    final var responseType = request.responseType();
                    logger.debug("{} <<< {}", this, responseBody);
                    return JacksonJsonUtils.toApiResponse(responseBody, responseType, request, httpResponse);
                })
                .thenApply(response -> {
                    if (!response.isSuccess()) {
                        throw new ApiException(response);
                    }
                    return response;
                });
    }

}
