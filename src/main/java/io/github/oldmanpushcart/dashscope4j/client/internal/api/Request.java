package io.github.oldmanpushcart.dashscope4j.client.internal.api;

import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 请求基类
 */
@EqualsAndHashCode
public abstract class Request {

    @ToString.Exclude
    private final Map<Class<?>, Object> contextMap;

    protected Request(Builder<?, ?> builder) {
        this.contextMap = builder.contextMap;
    }

    /**
     * 获取上下文
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

    /**
     * 获取上下文集合
     * <p>
     * 虽然开放这个出来很危险，但可以帮助跟踪系统做一些跟踪埋点。
     * 请慎用！
     * </p>
     *
     * @return 上下文集合
     * @since 3.1.1
     */
    public Map<Class<?>, Object> contextMap() {
        return contextMap;
    }

    public static abstract class Builder<T extends Request, B extends Builder<T, B>> implements Buildable<T, B> {

        private final Map<Class<?>, Object> contextMap = new HashMap<>();

        public Builder() {

        }

        public Builder(Request request) {
            this.contextMap.putAll(request.contextMap);
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

        /**
         * 从其他请求复制上下文
         * <p>
         * 用于请求接力的场景
         * </p>
         *
         * @param request 请求
         * @return this
         * @since 3.2.0
         */
        public B copyContextFrom(Request request) {
            this.contextMap.putAll(request.contextMap);
            return self();
        }

    }

}
