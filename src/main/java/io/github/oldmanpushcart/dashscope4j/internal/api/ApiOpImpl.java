package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.internal.CompletableFutureCallback;
import io.github.oldmanpushcart.dashscope4j.internal.task.TaskCancelRequest;
import io.github.oldmanpushcart.dashscope4j.internal.task.TaskGetRequest;
import io.github.oldmanpushcart.dashscope4j.internal.task.TaskGetResponse;
import io.github.oldmanpushcart.dashscope4j.internal.task.TaskHalfResponse;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import io.github.oldmanpushcart.dashscope4j.task.TaskException;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.*;
import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.failedStage;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ApiOpImpl implements ApiOp {

    private final String ak;
    private final OkHttpClient http;

    public ApiOpImpl(String ak, OkHttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    private Request newDelegateHttpRequest(ApiRequest<?> request, Consumer<Request.Builder> consumer) {
        final Request httpRequest = request.newHttpRequest();
        final Request.Builder builder = new Request.Builder(httpRequest)
                .addHeader(HTTP_HEADER_CONTENT_TYPE, "application/json")
                .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION);

        /*
         * 如果有设置AK，这里才主动设置AUTHORIZATION
         * 否则应该尽量依靠外部的设置
         */
        if (Objects.nonNull(ak)) {
            builder.addHeader(HTTP_HEADER_AUTHORIZATION, String.format("Bearer %s", ak));
        }

        consumer.accept(builder);
        return builder.build();
    }

    private Request newDelegateHttpRequest(ApiRequest<?> request) {
        return newDelegateHttpRequest(request, b -> {

        });
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<R> executeAsync(T request) {

        final Request httpRequest = newDelegateHttpRequest(request, builder -> builder
                .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
        );

        final CompletableFutureCallback<R> callback =
                new CompletableFutureCallback<>((call, httpResponse) -> {
                    final String bodyJson = requireNonNull(httpResponse.body()).string();
                    final R response = request.newResponseDecoder().apply(httpResponse, bodyJson);
                    if (!response.isSuccess()) {
                        throw new ApiException(httpResponse.code(), response);
                    }
                    return response;
                });

        http.newCall(httpRequest).enqueue(callback);
        return callback

                // 回填请求信息
                .thenApply(response -> response.fill(request));

    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Flowable<R>> executeFlow(T request) {

        final Request httpRequest = newDelegateHttpRequest(request, builder -> builder
                .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
        );

        final Flowable<R> flow = Flowable.create(emitter -> {

            final EventSource source = EventSources.createFactory(http).newEventSource(httpRequest, new EventSourceListener() {

                private volatile okhttp3.Response httpResponse;

                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response httpResponse) {
                    this.httpResponse = httpResponse;
                }

                @Override
                public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                    try {
                        if ("result".equals(type) || "error".equals(type)) {

                            // 构造应答
                            final R response = request.newResponseDecoder()
                                    .apply(httpResponse, data)
                                    .fill(request);

                            if (response.isSuccess()) {
                                emitter.onNext(response);
                            } else {
                                throw new ApiException(httpResponse.code(), response);
                            }
                        } else {
                            throw new IllegalStateException(String.format("Unexpected event type: %s", type));
                        }
                    } catch (Throwable cause) {
                        if (!emitter.isCancelled()) {
                            emitter.onError(cause);
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                    if (!emitter.isCancelled()) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    if (!emitter.isCancelled()) {
                        emitter.onComplete();
                    }
                }

            });

            emitter.setDisposable(Disposable.fromAction(source::cancel));

        }, BackpressureStrategy.BUFFER);
        return CompletableFuture.completedFuture(flow);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Exchange<T>> executeExchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {
        final CompletableFuture<Exchange<T>> exchangeF = new CompletableFuture<>();
        final String uuid = UUID.randomUUID().toString();

        // 请求编码器
        final Function<T, String> encoder = JacksonJsonUtils::toJson;

        // 应答解码器
        final BiFunction<okhttp3.Response, String, R> decoder = (response, responseJson) ->
                request.newResponseDecoder()
                        .apply(response, responseJson)
                        .fill(request);

        final WebSocketListener wsListener = new ExchangeWebSocketListenerImpl<>(
                exchangeF,
                uuid,
                mode,
                listener,
                encoder,
                decoder
        );

        final Request httpRequest = newDelegateHttpRequest(request);
        http.newWebSocket(httpRequest, wsListener);
        return exchangeF;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>> CompletionStage<Task.Half<R>> executeTask(T request) {

        final Request httpRequest = newDelegateHttpRequest(request, builder -> builder
                .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, ENABLE)
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
        );

        final CompletableFutureCallback<Task.Half<R>> callback =
                new CompletableFutureCallback<>((call, httpResponse) -> {

                    final TaskHalfResponse halfResponse = JacksonJsonUtils
                            .toObject(requireNonNull(httpResponse.body()).string(), TaskHalfResponse.class)
                            .fill(request);

                    if (!halfResponse.isSuccess()) {
                        throw new ApiException(httpResponse.code(), halfResponse);
                    }

                    final TaskGetRequest taskGetRequest = TaskGetRequest.newBuilder()
                            .taskId(halfResponse.output().taskId())
                            .context(request.context())
                            .build();

                    final Function<String, R> decoder = json ->
                            request.newResponseDecoder()
                                    .apply(httpResponse, json)
                                    .fill(request);

                    return strategy -> rollingTask(taskGetRequest, strategy, decoder);
                });

        http.newCall(httpRequest).enqueue(callback);

        return callback;
    }

    /**
     * 滚动任务执行，直至完结（成功、取消、失败）
     *
     * @param request  获取任务请求
     * @param strategy 滚动等待策略
     * @param finisher 任务结束处理器
     * @param <R>      应答类型
     * @return 任务应答
     */
    private <R> CompletionStage<R> rollingTask(TaskGetRequest request, Task.WaitStrategy strategy, Function<String, R> finisher) {
        return _rollingTask(request, strategy)
                .thenApply(response -> finisher.apply(response.raw()));
    }

    // 滚动任务执行，直至完结（成功、取消、失败）
    private CompletionStage<TaskGetResponse> _rollingTask(TaskGetRequest taskGetRequest, Task.WaitStrategy strategy) {
        return executeAsync(taskGetRequest)
                .thenCompose(taskGetResponse -> {

                    // 获取任务
                    final Task task = taskGetResponse.output().task();

                    // 任务取消
                    if (task.status() == Task.Status.CANCELED) {
                        throw new TaskException.TaskCancelledException(task.identity());
                    }

                    // 任务失败
                    if (task.status() == Task.Status.FAILED) {
                        throw new TaskException.TaskFailedException(task.identity(), taskGetResponse);
                    }

                    // 任务成功
                    if (task.status() == Task.Status.SUCCEEDED) {
                        return CompletableFuture.completedFuture(taskGetResponse);
                    }

                    // 任务继续
                    return strategy.performWait(task)

                            .handle((unused, ex) -> {

                                if (isNull(ex)) {
                                    return CompletableFuture.completedFuture(null);
                                }

                                if (!task.isCancelable()) {
                                    return failedStage(ex);
                                }

                                final TaskCancelRequest taskCancelRequest = TaskCancelRequest.newBuilder()
                                        .taskId(task.identity())
                                        .context(taskGetRequest.context())
                                        .build();
                                return executeAsync(taskCancelRequest)
                                        .handle((cv, cex) -> {
                                            log.warn("dashscope://task/cancel completed: task={};", task.identity(), cex);
                                            return cv;
                                        })
                                        .thenCompose(cv -> failedStage(ex));

                            })
                            .thenCompose(f -> f)

                            // 继续轮询
                            .thenCompose(unused -> _rollingTask(taskGetRequest, strategy));
                });
    }

}
