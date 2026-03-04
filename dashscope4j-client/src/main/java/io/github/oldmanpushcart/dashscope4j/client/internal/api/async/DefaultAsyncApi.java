package io.github.oldmanpushcart.dashscope4j.client.internal.api.async;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import okhttp3.*;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class DefaultAsyncApi implements AsyncApi, InternalContents {

    private final String host;
    private final String ak;
    private final OkHttpClient http;

    public DefaultAsyncApi(String host, String ak, OkHttpClient http) {
        this.host = host;
        this.ak = ak;
        this.http = http;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {
        final var httpRequest = new Request.Builder(request.toHttpRequest(host))
                .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();

        return CompletableFuture.completedStage(null)

                // 执行 HTTP 请求
                .thenCompose(unused -> {
                    final var completeF = new CompletableFuture<Response>();
                    http.newCall(httpRequest).enqueue(new Callback() {

                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            completeF.completeExceptionally(e);
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response httpResponse) {
                            completeF.complete(httpResponse);
                        }

                    });
                    return completeF;
                })

                // 解码 HTTP-RESPONSE
                .thenCompose(httpResponse -> {
                    try {
                        final var stringResponseBody = httpResponse.body().string();
                        final var response = request.responseDecoder().apply(httpResponse, stringResponseBody);
                        if (!response.isSuccess()) {
                            throw new ApiException(response);
                        }
                        return CompletableFuture.completedFuture(response);
                    } catch (Throwable ex) {
                        return CompletableFuture.failedFuture(ex);
                    }
                });
    }

}
