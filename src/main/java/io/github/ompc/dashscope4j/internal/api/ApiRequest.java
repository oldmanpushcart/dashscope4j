package io.github.ompc.dashscope4j.internal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.internal.util.Buildable;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * API请求
 *
 * @param <D> 数据类型
 */
public abstract class ApiRequest<D extends ApiData> {

    @JsonProperty("input")
    private final D data;

    @JsonProperty("parameters")
    private final Option option;

    private final Duration timeout;

    /**
     * 构造API请求
     *
     * @param builder 构造器
     */
    protected ApiRequest(Builder<D, ?, ?> builder) {
        this.data = builder.data;
        this.option = builder.option;
        this.timeout = builder.timeout;
    }

    /**
     * 获取数据
     *
     * @return 数据
     */
    public D data() {
        return data;
    }

    /**
     * 获取选项
     *
     * @return 选项
     */
    public Option option() {
        return option;
    }

    /**
     * 获取请求超时
     *
     * @return 请求超时
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * 构造器
     *
     * @param <D> 数据类型
     * @param <R> 请求类型
     * @param <B> 构造器类型
     */
    protected static abstract class Builder<D extends ApiData, R extends ApiRequest<D>, B extends Builder<D, R, B>> implements Buildable<R, B> {

        private final D data;
        private Duration timeout;
        private final Option option = new Option();

        /**
         * 构造构造器
         *
         * @param data 数据
         */
        protected Builder(D data) {
            this.data = data;
        }

        /**
         * 获取数据
         *
         * @return 数据
         */
        protected D input() {
            return data;
        }

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

        /**
         * 设置选项
         *
         * @param opt   选项
         * @param value 选项值
         * @param <OT>  选项类型
         * @param <OR>  选项值类型
         * @return this
         */
        public <OT, OR> B option(Option.Opt<OT, OR> opt, OT value) {
            option.option(opt, value);
            return self();
        }

        /**
         * 设置选项
         *
         * @param name  选项名
         * @param value 选项值
         * @return this
         */
        public B option(String name, Object value) {
            option.option(name, value);
            return self();
        }

    }

}
