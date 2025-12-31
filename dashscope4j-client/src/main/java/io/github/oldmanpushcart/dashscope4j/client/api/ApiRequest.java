package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiFunction;

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
     * 构建 HttpRequest
     * <p>
     * 允许实现者自定义实现HTTP请求，DashScope协议要求了多种方式（GET、POST）。
     * 不同的协议下采用的方式不一样，所以这里直接将HTTP请求的构造开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code T -> JSON}</p>
     *
     * @param host 服务地址
     * @return 构建{@code HTTP}请求
     */
    abstract public HttpRequest toHttpRequest(String host);

    /**
     * 构建应答解码器
     * <p>
     * 允许实现者自定义应答解码器，DashScope协议要求了多种应答格式（JSON、XML）。
     * 不同的协议下采用的方式不一样，所以这里直接将应答解码开放出来，确保足够的灵活性。
     * </p>
     * <p>{@code String -> T}</p>
     *
     * @return 构建应答解码器
     */
    abstract public BiFunction<HttpResponse<?>, String, R> responseDecoder();

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
