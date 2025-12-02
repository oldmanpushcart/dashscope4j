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

    /**
     * 构造请求
     *
     * @param responseType 响应类型
     * @param builder      构建者
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType is null!");
        this.responseType = responseType;
    }

    /**
     * @return 响应类型
     */
    public Class<R> responseType() {
        return responseType;
    }

    /**
     * 请求构建器
     *
     * @param <T> 构建目标类型
     * @param <B> 构建者类型
     */
    public static abstract class Builder<T extends ApiRequest<?>, B extends Builder<T, B>> implements Buildable<T, B> {

        protected Builder() {

        }

        protected Builder(ApiRequest<?> request) {

        }

    }

}
