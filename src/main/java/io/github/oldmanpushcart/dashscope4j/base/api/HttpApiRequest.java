package io.github.oldmanpushcart.dashscope4j.base.api;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

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

    /**
     * 构造器
     *
     * @param <T> 请求类型
     * @param <B> 构造器类型
     */
    interface Builder<T extends HttpApiRequest<?>, B extends Builder<T, B>>
            extends ApiRequest.Builder<T, B> {

    }

}
