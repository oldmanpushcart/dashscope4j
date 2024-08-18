package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.*;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.base.task.TaskException;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpSsEvent;
import io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpSsEventFlowPublisher;
import io.github.oldmanpushcart.internal.dashscope4j.base.exchange.ExchangeListenerAdapter;
import io.github.oldmanpushcart.internal.dashscope4j.base.task.TaskCancelRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.task.TaskGetRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.task.TaskGetResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.task.TaskHalfResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.Building;
import io.github.oldmanpushcart.internal.dashscope4j.util.HttpUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.MapFlowProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.github.oldmanpushcart.dashscope4j.Constants.*;
import static io.github.oldmanpushcart.internal.dashscope4j.base.api.http.HttpHeader.*;
import static io.github.oldmanpushcart.internal.dashscope4j.util.HttpUtils.loggingHttpRequest;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

/**
 * API执行器（HTTP实现）
 */
public class HttpApiExecutor implements ApiExecutor {

    private static final String CLIENT_INFO = "dashscope4j/%s".formatted(Constants.VERSION);
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final String ak;
    private final HttpClient http;
    private final Executor executor;
    private final Duration timeout;

    /**
     * 构造API执行器
     *
     * @param ak       AK
     * @param http     HTTP客户端
     * @param executor 线程池
     * @param timeout  超时
     */
    public HttpApiExecutor(String ak, HttpClient http, Executor executor, Duration timeout) {
        this.ak = ak;
        this.http = http;
        this.executor = executor;
        this.timeout = timeout;
    }

    // 创建委派HTTP请求构建器
    private HttpRequest.Builder newDelegateHttpRequestBuilder(HttpApiRequest<?> request) {
        return Building.of(HttpRequest.newBuilder(request.newHttpRequest(), (k, v) -> true))
                .acceptIfNotNull(timeout, HttpRequest.Builder::timeout)
                .acceptIfNotNull(request.timeout(), HttpRequest.Builder::timeout)
                .apply()
                .header(HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HEADER_X_DASHSCOPE_CLIENT, CLIENT_INFO);
    }

    /**
     * 异步执行API请求
     *
     * @param request 请求
     * @return 异步应答
     */
    @Override
    public <R extends HttpApiResponse<?>> CompletableFuture<R> async(HttpApiRequest<R> request) {

        // 构建委派请求
        final var delegateHttpRequest = newDelegateHttpRequestBuilder(request)
                .header(HEADER_X_DASHSCOPE_SSE, DISABLE)
                .build();

        // 记录请求日志
        loggingHttpRequest(delegateHttpRequest);

        // 发送请求
        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(identity(), executor)
                .whenComplete(HttpUtils::loggingHttpResponse)
                .thenApply(request.newHttpResponseHandler())
                .thenApply(httpResponse -> {
                    final var response = request.newResponseDecoder().apply(httpResponse.body());
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(httpResponse.statusCode(), response);
                    }
                    return response;
                });
    }


    // 在SSE事件的meta属性中提取HTTP状态提取匹配器
    private static final Pattern httpStatusPattern = Pattern.compile("HTTP_STATUS/(\\d+)");

    // 解析HTTP状态：HTTP_STATUS/429 -> 429
    private static int parseHttpStatus(HttpSsEvent event) {
        return event.metas().stream()
                .filter(meta -> meta.startsWith("HTTP_STATUS/"))
                .findFirst()
                .map(meta -> {
                    final var matcher = httpStatusPattern.matcher(meta);
                    return matcher.find()
                            ? Integer.parseInt(matcher.group(1))
                            : null;
                })
                .orElse(200);
    }

    /**
     * 流式处理API请求
     *
     * @param request 请求
     * @return 流式应答
     */
    @Override
    public <R extends HttpApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(HttpApiRequest<R> request) {

        final var delegateHttpRequest = newDelegateHttpRequestBuilder(request)
                .header(HEADER_X_DASHSCOPE_SSE, ENABLE)
                .build();

        loggingHttpRequest(delegateHttpRequest);

        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofPublisher())
                .thenApplyAsync(identity(), executor)
                .whenComplete(HttpUtils::loggingHttpResponse)
                .thenApply(request.newHttpResponseHandler())

                // 从HTTP响应数据流转换为SSE事件流
                .thenApply(httpResponse -> {

                    // 解析CT
                    final var ct = HttpHeader.ContentType.parse(httpResponse.headers());

                    // 检查是否为SSE
                    if (!HttpHeader.ContentType.MIME_TEXT_EVENT_STREAM.equals(ct.mime())) {
                        throw new IllegalStateException("Illegal HTTP Content-Type! expect:%s, actual:%s".formatted(
                                HttpHeader.ContentType.MIME_TEXT_EVENT_STREAM,
                                ct.mime()
                        ));
                    }

                    // 获取字符编码
                    final var charset = HttpHeader.ContentType.parse(httpResponse.headers()).charset();

                    // 开始处理SSE事件流
                    return HttpSsEventFlowPublisher.ofByteBufferListFlowPublisher(
                            httpResponse.body(),
                            charset
                    );

                })

                // 从SSE事件流中转换为API应答流
                .thenApply(ssePublisher -> MapFlowProcessor.syncOneToOne(ssePublisher, event -> switch (event.type()) {
                            case "error" -> throw new ApiException(
                                    parseHttpStatus(event),
                                    request.newResponseDecoder().apply(event.data())
                            );
                            case "result" -> request.newResponseDecoder().apply(event.data());
                            default -> throw new RuntimeException("Unsupported event type: %s".formatted(event.type()));
                        })
                );

    }

    /**
     * 任务式处理API请求
     *
     * @param request 请求
     * @param <R>     应答类型
     * @return 任务应答
     */
    @Override
    public <R extends HttpApiResponse<?>> CompletableFuture<Task.Half<R>> task(HttpApiRequest<R> request) {

        final var delegateHttpRequest = newDelegateHttpRequestBuilder(request)
                .header(HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HEADER_X_DASHSCOPE_ASYNC, ENABLE)
                .header(HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .build();

        loggingHttpRequest(delegateHttpRequest);

        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(identity(), executor)
                .whenComplete(HttpUtils::loggingHttpResponse)
                .thenApply(request.newHttpResponseHandler())

                // 解析HTTP响应为任务半应答
                .thenApply(httpResponse -> {
                    final TaskHalfResponse response = JacksonUtils.toObject(httpResponse.body(), TaskHalfResponse.class);
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(httpResponse.statusCode(), response);
                    }
                    return response;
                })

                // 任务滚动执行直至完成
                .thenApply(response -> strategy -> rollingTask(
                        new TaskGetRequest.Builder()
                                .taskId(response.output().taskId())
                                .building(builder -> Optional.ofNullable(request.timeout()).ifPresent(builder::timeout))
                                .build(),
                        strategy,
                        request.newResponseDecoder()
                ));
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
    private <R> CompletableFuture<R> rollingTask(TaskGetRequest request, Task.WaitStrategy strategy, Function<String, R> finisher) {
        return _rollingTask(request, strategy)
                .thenApply(response -> finisher.apply(response.raw()));
    }

    // 滚动任务执行，直至完结（成功、取消、失败）
    private CompletableFuture<TaskGetResponse> _rollingTask(TaskGetRequest taskGetRequest, Task.WaitStrategy strategy) {
        return async(taskGetRequest)
                .thenCompose(taskGetResponse -> {

                    // 获取任务
                    final var task = taskGetResponse.output().task();

                    // 任务取消
                    if (task.status() == Task.Status.CANCELED) {
                        throw new TaskException.TaskCancelledException(task.id());
                    }

                    // 任务失败
                    if (task.status() == Task.Status.FAILED) {
                        throw new TaskException.TaskFailedException(task.id(), taskGetResponse.ret());
                    }

                    // 任务成功
                    if (task.status() == Task.Status.SUCCEEDED) {
                        return CompletableFuture.completedFuture(taskGetResponse);
                    }

                    // 任务继续
                    return strategy.performWait(task)

                            // 失败则取消任务
                            .exceptionallyCompose(ex -> {

                                if (!task.isCancelable()) {
                                    return failedFuture(ex);
                                }

                                final var taskCancelRequest = new TaskCancelRequest.Builder()
                                        .taskId(task.id())
                                        .build();
                                return async(taskCancelRequest)
                                        .handle((cv, cex) -> {
                                            logger.warn("dashscope://task/cancel completed: task={};", task.id(), cex);
                                            return cv;
                                        })
                                        .thenCompose(cv -> failedFuture(ex));

                            })

                            // 继续轮询
                            .thenCompose(unused -> _rollingTask(taskGetRequest, strategy));
                });
    }

    @Override
    public <T extends ExchangeApiRequest<R>, R extends ExchangeApiResponse<?>>
    CompletableFuture<Exchange<T, R>> exchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {

        final URI remote = request instanceof AlgoRequest<?> algoRequest
                ? algoRequest.model().remote()
                : WSS_REMOTE;

        final var uuid = UUID.randomUUID().toString();
        final var exchangeListener = new ExchangeListenerAdapter<>(
                uuid,
                mode,
                r -> request.newExchangeRequestEncoder(uuid).apply(r),
                s -> request.newExchangeResponseDecoder(uuid).apply(s),
                listener
        );

        return Building.of(http.newWebSocketBuilder())
                .accept(builder -> builder.header(HEADER_AUTHORIZATION, ak))
                .acceptIfNotNull(timeout, WebSocket.Builder::connectTimeout)
                .acceptIfNotNull(request.timeout(), WebSocket.Builder::connectTimeout)
                .apply()
                .buildAsync(remote, exchangeListener)
                .thenCompose(v -> exchangeListener.getExchange())
                .thenCompose(exchange -> exchange.write(request));
    }


}
