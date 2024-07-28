package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.retry;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.util.ExceptionUtils;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.retry.RetryInterceptorImpl;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * 重试拦截器
 */
public interface RetryInterceptor extends Interceptor {

    /**
     * 重试匹配器
     */
    @FunctionalInterface
    interface Matcher {

        /**
         * 匹配
         *
         * @param context 调用上下文
         * @param request 请求
         * @param ex      异常
         * @return 是否匹配
         */
        boolean matches(InvocationContext context, ApiRequest<?> request, Throwable ex);

        /**
         * 与
         *
         * @param after 后续匹配器
         * @return 匹配器
         */
        default Matcher andThen(Matcher after) {
            return (c, r, ex) -> matches(c, r, ex) && after.matches(c, r, ex);
        }

        /**
         * 总是匹配
         *
         * @return 匹配器
         * @since 2.1.1
         */
        static Matcher alwaysTrue() {
            return (c, r, ex) -> true;
        }

        /**
         * 通过协议匹配
         *
         * @param filter 过滤器
         * @return 匹配器
         */
        static Matcher byProtocol(Predicate<String> filter) {
            return (c, r, ex) -> filter.test(r.protocol());
        }

        /**
         * 通过请求匹配
         *
         * @param filter 过滤器
         * @return 匹配器
         */
        static Matcher byRequest(Predicate<? super ApiRequest<?>> filter) {
            return (c, r, ex) -> filter.test(r);
        }

        /**
         * 通过请求匹配
         *
         * @param type 请求类型
         * @return 匹配器
         */
        static Matcher byRequest(Class<? extends ApiRequest<?>> type) {
            return (c, r, ex) -> type.isInstance(r);
        }

        /**
         * 通过异常匹配
         *
         * @param exceptionType 异常类型
         * @return 匹配器
         */
        static <X extends Throwable> Matcher byException(Class<? extends X> exceptionType, Predicate<? super X> filter) {
            return (c, r, ex) -> ExceptionUtils.isCauseBy(ex, exceptionType, filter);
        }

        /**
         * 通过 {@link ApiException} 异常匹配
         *
         * @param filter 过滤器
         * @return 匹配器
         */
        static Matcher byApiException(Predicate<? super ApiException> filter) {
            return (c, r, ex) -> ExceptionUtils.isCauseBy(ex, ApiException.class, filter);
        }

    }

    /**
     * @return 构造器
     */
    static Builder newBuilder() {
        return new RetryInterceptorImpl.Builder();
    }

    /**
     * 构造器
     */
    interface Builder extends Buildable<RetryInterceptor, Builder> {

        /**
         * 设置匹配器
         *
         * @param matcher 匹配器
         * @return this
         */
        Builder matches(Matcher matcher);

        /**
         * 设置最大重试次数
         *
         * @param maxRetries 最大重试次数
         * @return this
         */
        Builder maxRetries(int maxRetries);

        /**
         * 设置重试间隔
         *
         * @param retryInterval 重试间隔
         * @return this
         */
        Builder retryInterval(Duration retryInterval);

    }

}
