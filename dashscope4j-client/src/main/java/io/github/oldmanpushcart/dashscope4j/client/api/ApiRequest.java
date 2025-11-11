package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * 请求
 */
public abstract class ApiRequest<R extends ApiResponse> {

    private final Class<R> responseType;
    private final Map<Class<?>, Object> contextMap;

    /**
     * 构造请求
     *
     * @param responseType 响应类型
     * @param builder      构建者
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType is null!");
        this.responseType = responseType;
        this.contextMap = builder.contextMap;
    }

    /**
     * @return 响应类型
     */
    protected Class<R> responseType() {
        return responseType;
    }

    /**
     * @return HTTP请求编码器
     */
    abstract public Function<ApiRequest<?>, HttpRequest> newHttpRequestEncoder();

    /**
     * @return HTTP应答解码器
     */
    abstract public BiFunction<HttpResponse<?>, String, R> newHttpResponseDecoder();

    /**
     * 获取上下文
     *
     * @param <C> 上下文类型
     * @return 上下文
     */
    @SuppressWarnings("unchecked")
    public <C> C context() {
        return (C) context(Object.class);
    }

    /**
     * 获取上下文
     *
     * @param type 上下文类型
     * @param <C>  上下文类型
     * @return 上下文
     */
    @SuppressWarnings("unchecked")
    public <C> C context(Class<C> type) {
        return (C) contextMap.get(type);
    }


    /**
     * 请求构建器
     *
     * @param <T> 构建目标类型
     * @param <B> 构建者类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private final Map<Class<?>, Object> contextMap = new HashMap<>();

        protected Builder() {

        }

        protected Builder(ApiRequest<?> request) {
            this.contextMap.putAll(request.contextMap);
        }

        /**
         * 设置上下文
         *
         * @param context 上下文
         * @return this
         */
        public B context(Object context) {
            return context(Object.class, context);
        }

        /**
         * 设置上下文
         *
         * @param type    上下文类型
         * @param context 上下文
         * @param <C>     上下文类型
         * @return this
         */
        public <C> B context(Class<C> type, C context) {
            if (Objects.isNull(context)) {
                this.contextMap.remove(type);
            } else {
                this.contextMap.put(type, context);
            }
            return self();
        }

    }

}
