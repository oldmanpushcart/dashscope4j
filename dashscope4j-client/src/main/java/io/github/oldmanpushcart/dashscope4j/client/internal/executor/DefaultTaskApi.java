package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.task.TaskCancelRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.task.TaskGetRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.task.TaskGetResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.task.TaskHalfResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.task.TaskException;
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
    private final String host;
    private final String ak;
    private final HttpClient http;
    private final AsyncApi asyncApi;

    public DefaultTaskApi(String host, String ak, HttpClient http, AsyncApi asyncApi) {
        this.host = host;
        this.ak = ak;
        this.http = http;
        this.asyncApi = asyncApi;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request) {
        try {

            final var httpRequest = HttpRequest.newBuilder(request.toHttpRequest(host), (n, v) -> true)
                    .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, ENABLE)
                    .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();
            traceLogHttpRequest(httpRequest);

            return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(httpResponse -> {
                        final var halfResponseBody = httpResponse.body();
                        logger.debug("dashscope4j-client://task/half <<< {}", halfResponseBody);
                        final var halfResponse = JacksonJsonUtils.toApiResponse(halfResponseBody, TaskHalfResponse.class, request, httpResponse);
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

    private <R> CompletionStage<R> rolling(TaskGetRequest request, Task.WaitStrategy strategy, Function<String, R> decoder) {
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
                        throw new TaskException.TaskFailedException(task.taskId(), response);
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
