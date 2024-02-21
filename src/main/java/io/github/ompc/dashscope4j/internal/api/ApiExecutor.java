package io.github.ompc.dashscope4j.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.internal.api.http.HttpHeader;
import io.github.ompc.dashscope4j.internal.api.http.HttpSsEventProcessor;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import io.github.ompc.dashscope4j.util.TransformFlowProcessor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_AUTHORIZATION;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_X_DASHSCOPE_SSE;
import static java.util.function.Function.identity;

/**
 * API执行器
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
public abstract class ApiExecutor<T extends ApiRequest<?>, R extends ApiResponse<?>> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
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
    protected ApiExecutor(String sk, HttpClient http, Executor executor) {
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
    public CompletableFuture<R> async(T request) {
        final var delegateHttpRequest = delegateHttpRequest(newHttpRequest(request), builder -> {
            builder.header(HEADER_AUTHORIZATION, "Bearer %s".formatted(sk));
            builder.header(HEADER_X_DASHSCOPE_SSE, "disable");
        });
        return http.sendAsync(delegateHttpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApplyAsync(identity(), executor)
                .thenApply(httpResponse -> {
                    final var response = deserializeResponse(httpResponse.body());
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(httpResponse.statusCode(), response.ret());
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
    public CompletableFuture<Flow.Publisher<R>> flow(T request) {
        final var delegateHttpRequest = delegateHttpRequest(newHttpRequest(request), builder -> {
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
                                // 解析Ret
                                JacksonUtils.toObject(mapper, event.data(), Ret.class)
                        );
                        // 数据事件，处理数据
                        case "result" -> responses.add(deserializeResponse(event.data()));
                        // 未知事件，抛出异常
                        default -> throw new RuntimeException("Unsupported event type: %s".formatted(event.type()));
                    }
                    return responses;
                }));
    }

    /**
     * 构造HTTP请求
     * <p>{@code request -> HttpRequest}</p>
     *
     * @param request 请求
     * @return HTTP请求
     */
    protected abstract HttpRequest newHttpRequest(T request);

    /**
     * 序列化应答
     * <p>{@code json -> R}</p>
     *
     * @param body 应答JSON
     * @return 应答
     */
    protected abstract R deserializeResponse(String body);

}
