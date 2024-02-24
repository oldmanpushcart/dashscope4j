package io.github.ompc.dashscope4j.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Task;
import io.github.ompc.dashscope4j.internal.api.http.HttpHeader;
import io.github.ompc.dashscope4j.internal.api.http.HttpSsEventProcessor;
import io.github.ompc.dashscope4j.internal.task.TaskException;
import io.github.ompc.dashscope4j.internal.task.TaskGetRequest;
import io.github.ompc.dashscope4j.internal.task.TaskGetResponse;
import io.github.ompc.dashscope4j.internal.task.TaskHalfResponse;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import io.github.ompc.dashscope4j.util.TransformFlowProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_AUTHORIZATION;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_X_DASHSCOPE_SSE;
import static java.util.function.Function.identity;

/**
 * API执行器
 */
public class ApiExecutor {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String sk;
    private final HttpClient http;
    private final Executor executor;

    /**
     * 构造API执行器
     *
     * @param sk       SK
     * @param http     HTTP客户端
     * @param executor 线程池
     */
    public ApiExecutor(String sk, HttpClient http, Executor executor) {
        this.sk = sk;
        this.http = http;
        this.executor = executor;
    }

    // 委派API请求
    private static HttpRequest delegateHttpRequest(HttpRequest request, Consumer<HttpRequest.Builder> consumer) {
        final var builder = HttpRequest.newBuilder(request, (k, v) -> true);
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * 异步执行API请求
     *
     * @param request 请求
     * @return 异步应答
     */
    public <R extends ApiResponse<?>> CompletableFuture<R> async(ApiRequest<R> request) {
        final var delegateHttpRequest = delegateHttpRequest(request.newHttpRequest(), builder -> {
            builder.header(HEADER_AUTHORIZATION, "Bearer %s".formatted(sk));
            builder.header(HEADER_X_DASHSCOPE_SSE, "disable");
        });
        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(identity(), executor)
                .thenApply(httpResponse -> {
                    final var response = request.responseDeserializer().apply(httpResponse.body());
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(httpResponse.statusCode(), response);
                    }
                    return response;
                });
    }

    /**
     * 流式处理API请求
     *
     * @param request 请求
     * @return 流式应答
     */
    public <R extends ApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(ApiRequest<R> request) {
        final var delegateHttpRequest = delegateHttpRequest(request.newHttpRequest(), builder -> {
            builder.header(HEADER_AUTHORIZATION, "Bearer %s".formatted(sk));
            builder.header(HEADER_X_DASHSCOPE_SSE, "enable");
        });
        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofPublisher())
                .thenApplyAsync(identity(), executor)

                // 从HTTP响应数据流转换为SSE事件流
                .thenApply(httpResponse -> HttpSsEventProcessor
                        .fromByteBuffers(HttpHeader.ContentType.parse(httpResponse.headers()).charset(), 10240)
                        .transform(httpResponse.body())
                )

                // 从SSE事件流中转换为API应答流
                .thenApply(ssePublisher -> TransformFlowProcessor.transform(ssePublisher, event -> {
                    final var responses = new ArrayList<R>();
                    switch (event.type()) {
                        // 异常事件，直接抛出异常
                        case "error" -> throw new ApiException(
                                // 解析HTTP状态：HTTP_STATUS/429
                                event.meta().stream()
                                        .filter(meta -> meta.startsWith("HTTP_STATUS/"))
                                        .map(meta -> Integer.parseInt(meta.substring("HTTP_STATUS/".length())))
                                        .findFirst()
                                        .orElse(200),
                                // 解析应答
                                request.responseDeserializer().apply(event.data())
                        );
                        // 数据事件，处理数据
                        case "result" -> request.responseDeserializer().apply(event.data());
                        // 未知事件，抛出异常
                        default -> throw new RuntimeException("Unsupported event type: %s".formatted(event.type()));
                    }
                    return responses;
                }));
    }


    public <R extends ApiResponse<?>> CompletableFuture<Task.Half<R>> task(ApiRequest<R> request) {
        final var delegateHttpRequest = delegateHttpRequest(request.newHttpRequest(), builder -> {
            builder.header(HEADER_AUTHORIZATION, "Bearer %s".formatted(sk));
            builder.header(HEADER_X_DASHSCOPE_SSE, "disable");
        });
        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(identity(), executor)
                .thenApply(httpResponse -> {
                    final var response = JacksonUtils.toObject(mapper, httpResponse.body(), TaskHalfResponse.class);
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(httpResponse.statusCode(), response);
                    }
                    return response;
                })
                .thenApply(response -> strategy -> rolling(
                        new TaskGetRequest.Builder()
                                .taskId(response.output().taskId())
                                .building(builder -> Optional.ofNullable(request.timeout()).ifPresent(builder::timeout))
                                .build(),
                        strategy,
                        request.responseDeserializer()
                ));
    }

    private <R> CompletableFuture<R> rolling(TaskGetRequest request, Task.WaitStrategy strategy, Function<String, R> finisher) {
        return _rolling(request, strategy)
                .thenApply(response -> finisher.apply(response.output().body()));
    }

    private CompletableFuture<TaskGetResponse> _rolling(TaskGetRequest request, Task.WaitStrategy strategy) {
        return async(request)
                .thenCompose(response -> {
                    final var task = response.output().task();

                    // 任务取消
                    if (task.status() == Task.Status.CANCELED) {
                        throw new TaskException.TaskCancelledException(task.id());
                    }

                    // 任务失败
                    if (task.status() == Task.Status.FAILED) {
                        throw new TaskException.TaskFailedException(
                                response.output().task().id(),
                                response.ret()
                        );
                    }

                    // 任务完成
                    return task.isCompleted()
                            ? CompletableFuture.completedFuture(response)
                            : strategy.until(task).thenCompose(unused -> _rolling(request, strategy));
                });
    }

}
