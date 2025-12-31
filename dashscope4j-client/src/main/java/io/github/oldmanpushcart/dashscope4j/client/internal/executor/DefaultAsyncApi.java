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
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class DefaultAsyncApi implements AsyncApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String ak;
    private final HttpClient http;

    public DefaultAsyncApi(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://async";
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(URI endpoint, T request) {
        final var requestBody = JacksonJsonUtils.toJson(request);
        final var httpRequest = HttpRequest.newBuilder()
                .uri(endpoint)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        logger.debug("{} >>> {}", this, requestBody);
        return sendAsync(request, httpRequest);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request, Function<T, HttpRequest> transformer) {
        final var httpRequest = transformer.apply(request);
        logger.debug("{} >>> {};{}", this, httpRequest.method(), httpRequest.uri());
        return sendAsync(request, httpRequest);
    }

    private <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> sendAsync(T request, HttpRequest httpRequest) {
        final var newHttpRequest = HttpRequest.newBuilder(httpRequest, (n, v) -> true)
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();
        return http.sendAsync(newHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    final var responseBody = httpResponse.body();
                    logger.debug("{} <<< {}", this, responseBody);
                    final var responseType = request.responseType();
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
