package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class HttpAsyncExecutor {

    private final String ak;
    private final HttpClient http;

    public HttpAsyncExecutor(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {
        final var encoder = request.newHttpRequestEncoder();
        final var decoder = request.newHttpResponseDecoder();
        final var httpRequest = HttpRequest.newBuilder(encoder.apply(request), (k, v) -> true)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> decoder.apply(httpResponse, httpResponse.body()))
                .thenApply(response -> {

                    if (!response.isSuccess()) {
                        throw new ApiException(response);
                    }

                    return response;
                });
    }

}
