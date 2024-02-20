package io.github.ompc.dashscope4j.internal.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.internal.api.http.HttpHeader;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_AUTHORIZATION;
import static java.util.Optional.ofNullable;

public abstract class ApiExecutor<DT extends ApiData, T extends ApiRequest<DT>, DR extends ApiData, R extends ApiResponse<DR>> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final String sk;
    private final HttpClient http;
    private final Executor executor;

    public ApiExecutor(String sk, HttpClient http, Executor executor) {
        this.sk = sk;
        this.http = http;
        this.executor = executor;
    }

    public CompletableFuture<R> execute(T request, Consumer<R> consumer) {
        final var httpRequest = delegateHttpRequest(request, newHttpRequest(request));
        final var httpResponseBodyHandler = delegateResponseBodyHandler(newHttpResponseBodyHandler(request));
        return http.sendAsync(httpRequest, httpResponseBodyHandler)
                .thenApplyAsync(HttpResponse::body, executor);
    }


    private HttpRequest delegateHttpRequest(T request, HttpRequest httpRequest) {

        // 构造新请求
        final var builder = HttpRequest.newBuilder(httpRequest, (k, v) -> true);

        // 如有，设置认证
        ofNullable(sk).ifPresent(v -> builder.header(HEADER_AUTHORIZATION, "Bearer %s".formatted(sk)));

        // 如有，设置超时
        ofNullable(request.timeout()).ifPresent(builder::timeout);

        // 构造请求
        return builder.build();
    }

    private HttpResponse.BodyHandler<R> delegateResponseBodyHandler(HttpResponse.BodyHandler<R> responseBodyHandler) {
        return info -> 200 != info.statusCode()

                // HTTP状态失败，需要转换为API异常
                ? newApiExceptionBodySubscriber(info)

                // HTTP状态正常，才继续处理
                : responseBodyHandler.apply(info);
    }

    private HttpResponse.BodySubscriber<R> newApiExceptionBodySubscriber(HttpResponse.ResponseInfo info) {
        final var ct = HttpHeader.ContentType.parse(info.headers());
        final var charset = ct.charset();
        return HttpResponse.BodySubscribers.fromSubscriber(
                HttpResponse.BodySubscribers.ofString(charset),
                subscriber -> {
                    final var body = subscriber.getBody().toCompletableFuture().join();
                    final var ret = JacksonUtils.toObject(mapper, body, Ret.class);
                    throw new ApiException(info.statusCode(), ret);
                }
        );
    }

    protected abstract HttpRequest newHttpRequest(T request);

    protected abstract HttpResponse.BodyHandler<R> newHttpResponseBodyHandler(T request);

}
