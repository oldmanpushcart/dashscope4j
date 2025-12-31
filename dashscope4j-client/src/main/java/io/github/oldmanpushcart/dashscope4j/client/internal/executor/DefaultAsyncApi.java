package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class DefaultAsyncApi implements AsyncApi {

    private final String host;
    private final String ak;
    private final HttpClient http;

    public DefaultAsyncApi(String host, String ak, HttpClient http) {
        this.host = host;
        this.ak = ak;
        this.http = http;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://async";
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {

        final var oriHttpRequest = request.toHttpRequest(host);
        final var httpRequest = HttpRequest.newBuilder(oriHttpRequest, (n, v) -> true)
                .header(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(httpResponse -> {
                    final var responseBody = httpResponse.body();
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
