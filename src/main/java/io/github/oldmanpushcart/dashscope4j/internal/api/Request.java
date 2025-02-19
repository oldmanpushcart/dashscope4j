package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.ToString;

/**
 * 请求基类
 */
public abstract class Request {

    @ToString.Exclude
    private final Object context;

    protected Request(Builder<?, ?> builder) {
        this.context = builder.context;
    }

    /**
     * 获取上下文
     * <p>
     * 上下文回跟随请求传递到对应的应答报文，可以通过{@link Response#context()}获取
     * </p>
     *
     * @param <C> 上下文类型
     * @return 上下文
     * @since 3.1.0
     */
    @SuppressWarnings("unchecked")
    public <C> C context() {
        return (C) context;
    }

    public static abstract class Builder<T extends Request, B extends Builder<T, B>> implements Buildable<T, B> {

        private Object context;

        public Builder() {

        }

        public Builder(Request request) {
            this.context = request.context;
        }

        /**
         * 设置上下文
         *
         * @param context 上下文
         * @return this
         * @since 3.1.0
         */
        public B context(Object context) {
            this.context = context;
            return self();
        }

    }

}
