package io.github.oldmanpushcart.dashscope4j.api;

import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import okhttp3.Response;

import java.util.function.BiFunction;

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
@EqualsAndHashCode
public abstract class ApiRequest<R extends ApiResponse<?>> {

    @ToString.Exclude
    @Getter(PROTECTED)
    private final Class<R> responseType;

    /**
     * 构建Api请求
     *
     * @param responseType 应答类型
     * @param builder      构建器
     */
    protected ApiRequest(Class<R> responseType, Builder<?, ?> builder) {
        requireNonNull(responseType, "responseType is required!");
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
    public static abstract class Builder<T extends ApiRequest<?>, B extends ApiRequest.Builder<T, B>> implements Buildable<T, B> {

        protected Builder() {

        }

        protected Builder(T request) {

        }

    }

}
