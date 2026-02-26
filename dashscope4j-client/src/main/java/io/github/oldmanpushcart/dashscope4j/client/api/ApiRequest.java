package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.api.intercetpor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * API 请求
 *
 * @param <R> 应答类型
 */
public abstract class ApiRequest<R extends ApiResponse> {

    private final Type responseType;
    private final List<Interceptor> interceptors;

    /**
     * 构造函数
     *
     * @param responseType 应答类型
     * @param builder      构建器
     */
    protected ApiRequest(Type responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType must not be null");
        requireNonNull(builder, "builder must not be null");
        this.responseType = responseType;
        this.interceptors = null != builder.interceptors
                ? Collections.unmodifiableList(builder.interceptors)
                : Collections.emptyList();
    }

    /**
     * @return 应答类型
     */
    public Type responseType() {
        return responseType;
    }

    /**
     * @return 拦截链
     */
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    /**
     * 构建 HttpRequest
     * <p>
     * 允许实现者自定义实现{@code HTTP}请求。DashScope协议要求了多种方式（GET、POST）。
     * 不同的协议下采用的方式不一样，所以这里直接将{@code HTTP}请求的构造开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code API -> HTTP}</p>
     *
     * @param host 主机名
     * @return {@code HTTP}请求
     */
    abstract public okhttp3.Request toHttpRequest(String host);

    /**
     * {@code HTTP}响应节码器
     * <p>
     * 允许实现者自定义应答解码。
     * </p>
     * <p>{@code (HTTP, BODY) -> R} </p>
     *
     * @return API 应答
     */
    abstract public BiFunction<okhttp3.Response, String, R> responseDecoder();


    /**
     * 构建器
     *
     * @param <T> API请求类型
     * @param <B> 构建器类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private List<Interceptor> interceptors;

        protected Builder() {

        }

        protected Builder(ApiRequest<?> request) {
            this.interceptors = request.interceptors;
        }

        /**
         * 设置拦截链
         * <p>
         * 拦截器将会按照集合顺序执行。
         * <ul>
         *     <li>请求拦截顺序：FIFO；{@code interceptor1 -> interceptor2 -> interceptor3}</li>
         *     <li>响应拦截顺序：LIFO；{@code interceptor3 -> interceptor2 -> interceptor1}</li>
         * </ul>
         *
         * </p>
         *
         * @param interceptors 拦截链
         * @return this
         */
        public B interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return self();
        }

    }

}
