package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.internal.util.Buildable;

import java.net.http.HttpRequest;
import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * API请求
 *
 */
public abstract class ApiRequest<R extends ApiResponse<?>> {

    private final Duration timeout;
    private final Class<R> responseType;

    /**
     * 构造API请求
     *
     * @param builder 构造器
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        this.timeout = builder.timeout;
        this.responseType = responseType;
    }

    /**
     * 获取请求超时
     *
     * @return 请求超时
     */
    public Duration timeout() {
        return timeout;
    }

    protected abstract HttpRequest newHttpRequest();

    Class<R> responseType() {
        return responseType;
    }


    /**
     * 构造器
     *
     * @param <T> 请求类型
     * @param <B> 构造器类型
     */
    protected static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        private Duration timeout;

        /**
         * 设置请求超时
         *
         * @param timeout 请求超时
         * @return this
         */
        public B timeout(Duration timeout) {
            this.timeout = requireNonNull(timeout);
            return self();
        }

    }

}
