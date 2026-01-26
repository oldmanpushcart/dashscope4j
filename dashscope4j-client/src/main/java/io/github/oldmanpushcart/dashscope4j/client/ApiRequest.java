package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * 请求
 */
public abstract class ApiRequest<R extends ApiResponse> {

    private final Type responseType;
    private final List<Interceptor> interceptors;

    protected ApiRequest(Type responseType) {
        this.responseType = responseType;
        this.interceptors = List.of();
    }

    /**
     * 构造请求
     *
     * @param responseType 响应类型
     * @param builder      构建者
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType is null!");
        this.responseType = responseType;
        this.interceptors = Collections.unmodifiableList(builder.interceptors);
    }

    /**
     * @return 响应类型
     */
    public Type responseType() {
        return responseType;
    }

    /**
     * @return 拦截器列表
     */
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    /**
     * 构建 HttpRequest
     * <p>
     * 允许实现者自定义实现HTTP请求，DashScope协议要求了多种方式（GET、POST）。
     * 不同的协议下采用的方式不一样，所以这里直接将HTTP请求的构造开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code T -> JSON}</p>
     *
     * @return 构建{@code HTTP}请求
     */
    abstract public HttpRequest toHttpRequest(String host);

    /**
     * 响应解码器
     * <p>
     * 允许实现者自定义响应解码器，DashScope协议要求了多种方式（JSON、BINARY）。
     * </p>
     * <p>{@code JSON -> R}</p>
     *
     * @return 响应解码器
     */
    abstract public BiFunction<HttpResponse<?>, String, R> responseDecoder();

    /**
     * 请求构建器
     *
     * @param <T> 构建目标类型
     * @param <B> 构建者类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private final List<Interceptor> interceptors = new ArrayList<>();

        protected Builder() {

        }

        protected Builder(ApiRequest<?> request) {
            interceptors.addAll(request.interceptors);
        }

        public B interceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors, "interceptors must not be null!");
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return self();
        }

        public B addInterceptor(Interceptor interceptor) {
            requireNonNull(interceptor, "interceptor must not be null!");
            interceptors.add(interceptor);
            return self();
        }

        public B addInterceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors, "interceptors must not be null!");
            this.interceptors.addAll(interceptors);
            return self();
        }

    }

}
