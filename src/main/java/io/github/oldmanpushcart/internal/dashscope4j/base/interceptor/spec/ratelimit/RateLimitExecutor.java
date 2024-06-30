package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;

import java.time.Duration;

/**
 * 限流执行器
 */
interface RateLimitExecutor {

    /**
     * 尝试申请令牌
     *
     * @param context 上下文
     * @param request 请求
     * @return 获取结果
     */
    Token tryAcquire(InvocationContext context, ApiRequest<?> request);

    /**
     * 令牌
     */
    interface Token {

        /**
         * 是否获取到令牌
         *
         * @return TRUE | FALSE
         */
        default boolean isAcquired() {
            return strategy() == RateLimiter.Strategy.PASS;
        }

        /**
         * @return 流控策略
         */
        RateLimiter.Strategy strategy();

        /**
         * <p>当{@link #isAcquired()}=={@code true}时为{@link Duration#ZERO}</p>
         * <p>当{@link #isAcquired()}=={@code false}时为下次再次申请令牌的延迟时长</p>
         *
         * @return 下次申请令牌的延迟时长
         */
        Duration delay();

        /**
         * 释放令牌：成功
         *
         * @param spend 已用量
         */
        void success(Usage spend);

        /**
         * 释放令牌：失败
         */
        void failure();

        /**
         * 释放令牌：撤销
         */
        void cancel();

    }

}
