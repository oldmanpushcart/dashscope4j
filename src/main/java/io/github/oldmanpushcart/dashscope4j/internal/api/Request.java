package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 请求基类
 */
public abstract class Request {

    @ToString.Exclude
    private final Map<Class<?>, Object> contextMap;

    protected Request(Builder<?, ?> builder) {
        this.contextMap = builder.contextMap;
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
        return (C) context(Object.class);
    }

    /**
     * 获取上下文
     *
     * @param type 上下文类型
     * @param <C>  上下文类型
     * @return 上下文
     * @since 3.1.0
     */
    @SuppressWarnings("unchecked")
    public <C> C context(Class<C> type) {
        return (C) contextMap.get(type);
    }

    protected Map<Class<?>, Object> contextMap() {
        return contextMap;
    }

    public static abstract class Builder<T extends Request, B extends Builder<T, B>> implements Buildable<T, B> {

        private final Map<Class<?>, Object> contextMap;

        public Builder() {
            this.contextMap = new HashMap<>();
        }

        public Builder(Request request) {
            this.contextMap = request.contextMap;
        }

        /**
         * 设置上下文
         *
         * @param context 上下文
         * @return this
         * @since 3.1.0
         */
        public B context(Object context) {
            return context(Object.class, context);
        }

        /**
         * 设置上下文
         *
         * @param type    上下文类型
         * @param context 上下文
         * @param <C>     上下文类型
         * @return this
         * @since 3.1.0
         */
        public <C> B context(Class<C> type, C context) {
            if (Objects.isNull(context)) {
                this.contextMap.remove(type);
            } else {
                this.contextMap.put(type, context);
            }
            return self();
        }

    }

}
