package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptorImpl;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 限流拦截器
 *
 * @since 1.4.3
 */
public interface RateLimitInterceptor extends Interceptor {

    /**
     * @return 限流拦截构建器
     */
    static Builder newBuilder() {
        return new RateLimitInterceptorImpl.Builder();
    }

    /**
     * 构造器
     */
    interface Builder extends Buildable<RateLimitInterceptor, Builder> {

        /**
         * 追加限流器
         *
         * @param limiters 限流器集合
         * @return this
         */
        default Builder limiters(RateLimiter... limiters) {
            return limiters(true, List.of(limiters));
        }

        /**
         * 追加限流器
         *
         * @param limiters 限流器集合
         * @return this
         */
        default Builder limiters(List<RateLimiter> limiters) {
            return limiters(true, limiters);
        }

        /**
         * 设置限流器
         *
         * @param isAppend 是否追加
         * @param limiters 限流器集合
         * @return this
         */
        Builder limiters(boolean isAppend, List<RateLimiter> limiters);

        /**
         * 设置延迟调度器
         *
         * @param scheduler 调度器
         * @return this
         */
        Builder scheduler(ScheduledExecutorService scheduler);

    }

}
