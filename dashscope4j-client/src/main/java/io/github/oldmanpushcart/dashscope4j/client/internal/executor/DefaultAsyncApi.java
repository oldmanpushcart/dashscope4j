package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils.traceLogHttpRequest;

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
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {

        try {

            final var httpRequest = HttpRequest.newBuilder(request.toHttpRequest(host), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();
            traceLogHttpRequest(httpRequest);

            return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .whenComplete(HttpUtils::traceLogHttpResponse)
                    .thenApply(httpResponse -> {
                        final var responseBody = httpResponse.body();
                        final var response = request.responseDecoder().apply(httpResponse, responseBody);
                        if (!response.isSuccess()) {
                            throw new ApiException(response);
                        }
                        return response;
                    });

        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }

    }

}
