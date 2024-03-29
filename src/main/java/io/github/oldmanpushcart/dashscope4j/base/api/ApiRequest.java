package io.github.oldmanpushcart.dashscope4j.base.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Function;

/**
 * API请求
 */
public interface ApiRequest<R extends ApiResponse<?>> {

    @JsonIgnore
    Duration timeout();

    /**
     * 转换为HTTP请求
     *
     * @return HTTP请求
     */
    HttpRequest newHttpRequest();

    /**
     * 应答序列化
     *
     * @return 应答序列化
     */
    Function<String, R> responseDeserializer();


    /**
     * 构造器
     *
     * @param <T> 请求类型
     * @param <B> 构造器类型
     */
    interface Builder<T extends ApiRequest<?>, B extends Builder<T, B>> extends Buildable<T, B> {

        /**
         * 设置请求超时
         *
         * @param timeout 请求超时
         * @return this
         */
        B timeout(Duration timeout);

    }

}
