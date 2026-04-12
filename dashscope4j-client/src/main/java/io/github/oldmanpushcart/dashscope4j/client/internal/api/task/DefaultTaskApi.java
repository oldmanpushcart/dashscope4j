package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.api.task.TaskException;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.Constants;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedStage;
import static java.util.concurrent.CompletableFuture.failedStage;

public class DefaultTaskApi implements TaskApi, InternalContents {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String host;
    private final String ak;
    private final OkHttpClient http;
    private final AsyncApi asyncApi;
    private final ExecutorService executor;

    public DefaultTaskApi(String host, String ak, OkHttpClient http, AsyncApi asyncApi, ExecutorService executor) {
        this.host = host;
        this.ak = ak;
        this.http = http;
        this.asyncApi = asyncApi;
        this.executor = executor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request) {

        final var httpRequest = new Request.Builder(request.toHttpRequest(host))
                .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();

        // 第1步：OkHttp 内部线程执行网络请求
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

        // 第2步：收到响应后切换到自定义 executor 处理业务逻辑
        return completeF.thenComposeAsync(httpResponse -> {
            try {
                final var stringResponseBody = httpResponse.body().string();
                final var halfResponse = JacksonJsonUtils.<TaskHalfResponse>toApiResponse(stringResponseBody, TaskHalfResponse.class, request, httpResponse);
                if (!halfResponse.isSuccess()) {
                    throw new ApiException(halfResponse);
                }
                final var taskId = halfResponse.output().taskId();
                final var half = new Task.Half<R>() {

                    @Override
                    public CompletionStage<R> waitingFor(Task.WaitStrategy strategy) {
                        return rolling(
                                new TaskGetRequest(taskId),
                                strategy,
                                responseBody -> request.responseDecoder().apply(httpResponse, responseBody)
                        );
                    }

                };
                return CompletableFuture.completedStage(half);
            } catch (Throwable ex) {
                return CompletableFuture.<Task.Half<R>>failedStage(ex);
            }
        }, executor);
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
