package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.api.intercetpor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.lang.reflect.Type;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * API 请求
 *
 * @param <R> 应答类型
 */
public abstract class ApiRequest<R extends ApiResponse> {

    private final Type responseType;
    private final List<Interceptor> interceptors;

    protected ApiRequest(Type responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType must not be null");
        requireNonNull(builder, "builder must not be null");
        this.responseType = responseType;
        this.interceptors = Collections.unmodifiableList(builder.interceptors);
    }

    /**
     * @return 应答类型
     */
    public Type responseType() {
        return responseType;
    }

    /**
     * @return 拦截器集合
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
     *
     * @return 构建{@code HTTP}请求
     */
    abstract public HttpRequest toHttpRequest(String host);

    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private final List<Interceptor> interceptors = new ArrayList<>();

    }

}
