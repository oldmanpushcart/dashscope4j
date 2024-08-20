package io.github.oldmanpushcart.dashscope4j.base.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;

import java.time.Duration;

/**
 * API请求
 */
public interface ApiRequest {

    /**
     * @return 协议
     */
    default String protocol() {
        return "%s/%s".formatted(suite(), type());
    }

    /**
     * @return 协议簇
     */
    default String suite() {
        return "dashscope://" + getClass().getPackageName();
    }

    /**
     * @return 协议类型
     */
    default String type() {
        return getClass().getSimpleName();
    }

    /**
     * @return 请求超时
     */
    @JsonIgnore
    Duration timeout();

    /**
     * 构造器
     *
     * @param <T> 请求类型
     * @param <B> 构造器类型
     */
    interface Builder<T extends ApiRequest, B extends Builder<T, B>> extends Buildable<T, B> {

        /**
         * 设置请求超时
         *
         * @param timeout 请求超时
         * @return this
         */
        B timeout(Duration timeout);

    }

}
