package io.github.oldmanpushcart.dashscope4j.base.api;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

/**
 * HTTP类Api请求
 *
 * @param <R> HTTP应答类型
 * @since 2.2.0
 */
public interface HttpApiRequest<R extends HttpApiResponse<?>> extends ApiRequest {

    /**
     * @return 构造HTTP请求
     */
    HttpRequest newHttpRequest();

    /**
     * @return HTTP应答处理器
     */
    default <T> Function<HttpResponse<T>, HttpResponse<T>> newHttpResponseHandler() {
        return Function.identity();
    }

    /**
     * @return 应答解码器
     */
    Function<String, R> newResponseDecoder();

}
