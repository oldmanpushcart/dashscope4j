package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiException;
import io.github.ompc.dashscope4j.internal.api.ApiExecutor;
import io.github.ompc.dashscope4j.internal.api.http.HttpHeader;
import io.github.ompc.dashscope4j.internal.api.http.HttpResponseEventSubscriber;
import io.github.ompc.dashscope4j.internal.util.Aggregatable;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_X_DASHSCOPE_SSE;


public abstract class AlgoExecutor<M extends Model, DT extends ApiData, T extends AlgoRequest<M, DT>, DR extends ApiData, R extends AlgoResponse<DR, R>> extends ApiExecutor<DT, T, DR, R> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final boolean stream;
    private final Class<R> responseType;

    protected AlgoExecutor(String sk, HttpClient http, Executor executor, boolean stream, Class<R> responseType) {
        super(sk, http, executor);
        this.stream = stream;
        this.responseType = responseType;
    }

    @Override
    final protected HttpRequest newHttpRequest(T request) {
        final var builder = HttpRequest.newBuilder()

                // 设置地址：算法的请求地址由模型决定
                .uri(request.model().remote())

                // 设置头部：算法请求的CT为JSON
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

        // 序列化请求
        final var json = JacksonUtils.toJson(mapper, request);
        logger.debug("{}/{}/http => {}", this, request.model().name(), json);

        // 设置请求BODY
        builder.POST(HttpRequest.BodyPublishers.ofString(json));

        // 设置是否支持SSE。算法请求具备处理SSE的能力
        builder.header(HEADER_X_DASHSCOPE_SSE, stream ? "enable" : "disable");

        return builder.build();
    }

    @Override
    final protected HttpResponse.BodyHandler<R> newHttpResponseBodyHandler(T request) {
        return info -> {
            final var ct = HttpHeader.ContentType.parse(info.headers());
            return switch (ct.mime()) {
                case HttpHeader.ContentType.MIME_APPLICATION_JSON -> newBlockBodySubscriber(request, ct);
                case HttpHeader.ContentType.MIME_TEXT_EVENT_STREAM -> newStreamBodySubscriber(request, info, ct);
                default -> throw new RuntimeException("Unsupported Content-Type: %s".formatted(ct.mime()));
            };
        };
    }

    private HttpResponse.BodySubscriber<R> newBlockBodySubscriber(T request, HttpHeader.ContentType ct) {
        return HttpResponse.BodySubscribers.fromSubscriber(
                HttpResponse.BodySubscribers.ofString(ct.charset()),
                subscriber -> {
                    final var body = subscriber.getBody().toCompletableFuture().join();
                    logger.debug("{}/{}/http <= {}", this, request.model().name(), body);
                    return JacksonUtils.toObject(mapper, body, responseType);
                }
        );
    }

    private HttpResponse.BodySubscriber<R> newStreamBodySubscriber(T request, HttpResponse.ResponseInfo info, HttpHeader.ContentType ct) {

        // 是否增量累加
        final var increment = stream && request.option().has(AlgoOptions.ENABLE_INCREMENTAL_OUTPUT, true);

        // 累加操作
        final var accumulateOp = Aggregatable.<R>accumulateOp(increment);

        // 响应引用
        final var responseRef = new AtomicReference<R>();

        // 流式订阅器
        final var subscriber = new HttpResponseEventSubscriber(ct.charset(), event -> {

            logger.debug("{}/{}/http <= {}", this, request.model().name(), event.data());

            // 异常事件，直接抛出
            if ("error".equals(event.type())) {

                // 解析HTTP状态：HTTP_STATUS/429
                final var status = event.meta().stream()
                        .filter(meta -> meta.startsWith("HTTP_STATUS/"))
                        .map(meta -> Integer.parseInt(meta.substring("HTTP_STATUS/".length())))
                        .findAny()
                        .orElse(info.statusCode());

                // 解析返回的错误信息
                final var ret = JacksonUtils.toObject(mapper, event.data(), Ret.class);

                // 抛出异常
                throw new ApiException(status, ret);

            }

            // 数据事件，处理数据
            else if ("result".equals(event.type())) {
                final var response = JacksonUtils.toObject(mapper, event.data(), responseType);
                responseRef.updateAndGet(r -> accumulateOp.apply(r, response));
            }

            // 未知事件，抛出异常
            else {
                throw new RuntimeException("Unsupported Event-Type: %s".formatted(event.type()));
            }

        });

        // 修改流式订阅器返回为最终累加后的响应
        return HttpResponse.BodySubscribers.mapping(subscriber, unused -> responseRef.get());
    }

}
