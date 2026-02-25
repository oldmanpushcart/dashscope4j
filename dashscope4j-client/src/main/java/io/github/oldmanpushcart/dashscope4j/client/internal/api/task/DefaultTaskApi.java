package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.api.task.TaskException;
import io.github.oldmanpushcart.dashscope4j.client.internal.Config;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.client.internal.util.HttpUtils.traceLogHttpRequest;
import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedStage;

public class DefaultTaskApi implements TaskApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Config config;
    private final HttpClient http;
    private final AsyncApi asyncApi;

    public DefaultTaskApi(Config config, HttpClient http, AsyncApi asyncApi) {
        this.config = config;
        this.http = http;
        this.asyncApi = asyncApi;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request) {
        try {

            final var builder = HttpRequest.newBuilder(request.toHttpRequest(config.host()), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(config.ak()))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, ENABLE)
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
                    .thenApply(httpResponse -> {
                        final var halfResponseBody = httpResponse.body();
                        logger.debug("dashscope4j-client://task/half <<< {}", halfResponseBody);
                        final var halfResponse = JacksonJsonUtils.<TaskHalfResponse>toApiResponse(halfResponseBody, TaskHalfResponse.class, request, httpResponse);
                        if (!halfResponse.isSuccess()) {
                            throw new ApiException(halfResponse);
                        }
                        final var taskId = halfResponse.output().taskId();
                        return strategy -> rolling(
                                new TaskGetRequest(taskId),
                                strategy,
                                responseBody -> request.responseDecoder().apply(httpResponse, responseBody)
                        );
                    });

        } catch (Throwable ex) {
            return failedStage(ex);
        }
    }

    private <R extends ApiResponse> CompletionStage<R> rolling(TaskGetRequest request, Task.WaitStrategy strategy, Function<String, R> decoder) {
        return _rolling(request, strategy)
                .thenApply(response -> decoder.apply(response.raw()));
    }

    private CompletionStage<TaskGetResponse> _rolling(TaskGetRequest request, Task.WaitStrategy strategy) {
        return asyncApi.execute(request)
                .thenCompose(response -> {

                    final var task = response.task();

                    if (task.status() == Task.Status.CANCELED) {
                        throw new TaskException.TaskCancelledException(task.taskId());
                    }

                    if (task.status() == Task.Status.FAILED) {
                        throw new TaskException.TaskFailedException(task);
                    }

                    if (task.status() == Task.Status.SUCCEEDED) {
                        return completedStage(response);
                    }

                    return strategy.performWait(task)
                            .handle((unused, ex) -> {

                                if (null == ex) {
                                    return CompletableFuture.completedStage(null);
                                }

                                if (!task.isCancelable()) {
                                    return failedStage(ex);
                                }

                                return asyncApi.execute(new TaskCancelRequest(task.taskId()))
                                        .thenCompose(cancelResponse -> failedStage(ex));

                            })
                            .thenCompose(v -> v)
                            .thenCompose(v -> _rolling(request, strategy));

                });
    }

}
