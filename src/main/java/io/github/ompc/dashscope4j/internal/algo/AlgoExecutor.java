package io.github.ompc.dashscope4j.internal.algo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ompc.dashscope4j.internal.api.ApiExecutor;
import io.github.ompc.dashscope4j.internal.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.concurrent.Executor;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.HEADER_CONTENT_TYPE;

/**
 * 算法执行器
 *
 * @param <T> 请求
 * @param <R> 响应
 */
public abstract class AlgoExecutor<T extends AlgoRequest<?, ?>, R extends AlgoResponse<?>> extends ApiExecutor<T, R> {

    private static final ObjectMapper mapper = JacksonUtils.mapper();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Class<R> responseType;

    protected AlgoExecutor(String sk, HttpClient http, Executor executor, Class<R> responseType) {
        super(sk, http, executor);
        this.responseType = responseType;
    }

    @Override
    protected HttpRequest newHttpRequest(T request) {
        final var builder = HttpRequest.newBuilder()

                // 设置地址：算法的请求地址由模型决定
                .uri(request.model().remote())

                // 设置头部：算法请求的CT为JSON
                .header(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);

        // 序列化请求
        final var json = JacksonUtils.toJson(mapper, request);
        logger.debug("{}/api => {}", this, json);

        // 设置请求BODY
        builder.POST(HttpRequest.BodyPublishers.ofString(json));

        return builder.build();
    }

    @Override
    protected R deserializeResponse(String body) {
        logger.debug("{}/api <= {}", this, body);
        return JacksonUtils.toObject(mapper, body, responseType);
    }

}
