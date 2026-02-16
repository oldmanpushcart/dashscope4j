package io.github.oldmanpushcart.dashscope4j.client.internal.api.async;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.Config;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils.traceLogHttpRequest;

public class DefaultAsyncApi implements AsyncApi {

    private final Config config;
    private final HttpClient http;

    public DefaultAsyncApi(Config config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request) {

        try {

            final var builder = HttpRequest.newBuilder(request.toHttpRequest(config.host()), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(config.ak()))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE);

            if (config.httpTimeout() != null) {
                builder.timeout(config.httpTimeout());
            }

            final var httpRequest = builder.build();
            traceLogHttpRequest(httpRequest);

            //noinspection resource
            final var scope = Tracer.instance.enter("http");
            scope.span()
                    .property("method", httpRequest.method())
                    .property("uri", httpRequest.uri().toString());
            return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((r, ex) -> {
                        final var span = scope.restore();
                        if (null == ex) {
                            span.success()
                                    .property("http-status", String.valueOf(r.statusCode()))
                                    .property("http-version", String.valueOf(r.version()))
                                    .property("http-content-type", r.headers().firstValue(HTTP_HEADER_CONTENT_TYPE).orElse(""));
                        } else {
                            span.failure(ex);
                        }
                        scope.close();
                    })
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
