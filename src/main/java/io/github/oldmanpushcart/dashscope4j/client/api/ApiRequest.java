package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.Request;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;

/**
 * API请求
 *
 * @param <R> 应答类型
 */
@Getter
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class ApiRequest<R extends ApiResponse<?>> extends Request {

    @ToString.Exclude
    @Getter(PROTECTED)
    private final Class<R> responseType;

    @ToString.Exclude
    private final List<Interceptor> interceptors;

    /**
     * 构建Api请求
     *
     * @param responseType 应答类型
     * @param builder      构建器
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        super(builder);
        requireNonNull(responseType, "responseType is required!");
        this.responseType = responseType;
        this.interceptors = unmodifiableList(builder.interceptors);
    }

    /**
     * 构建 HttpRequest
     * <p>
     * 允许实现者自定义实现HTTP请求，DashScope协议要求了多种方式（GET、POST）。
     * 不同的协议下采用的方式不一样，所以这里直接将HTTP请求的构造开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code T -> JSON}</p>
     *
     * @return 构建HTTP请求
     */
    abstract public okhttp3.Request newHttpRequest();

    /**
     * 构建 Response 解码器
     * <p>{@code JSON -> R}</p>
     *
     * @return Response 解码器
     */
    abstract public BiFunction<Response, String, R> newResponseDecoder();

    /**
     * API请求构造器
     *
     * @param <T> 请求类型
     * @param <B> 构造器类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> extends Request.Builder<T, B> {

        private final List<Interceptor> interceptors = new ArrayList<>();

        protected Builder() {

        }

        protected Builder(ApiRequest<?> request) {
            super(request);
            this.interceptors.addAll(request.interceptors);
        }

        /**
         * 设置拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         * @since 3.1.1
         */
        public B interceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加拦截器
         *
         * @param interceptor 拦截器
         * @return this
         * @since 3.1.1
         */
        public B addInterceptor(Interceptor interceptor) {
            requireNonNull(interceptor);
            this.interceptors.add(interceptor);
            return self();
        }

        /**
         * 添加拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         * @since 3.1.1
         */
        public B addInterceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 根据拦截器类型移除
         *
         * @param interceptorType 拦截器类型
         * @return this
         * @since 3.2.0
         */
        public B removeInterceptorByType(Class<? extends Interceptor> interceptorType) {
            requireNonNull(interceptorType);
            this.interceptors.removeIf(interceptorType::isInstance);
            return self();
        }

    }

}
