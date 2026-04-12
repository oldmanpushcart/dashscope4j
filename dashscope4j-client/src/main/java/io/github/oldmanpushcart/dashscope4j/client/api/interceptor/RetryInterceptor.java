package io.github.oldmanpushcart.dashscope4j.client.api.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategies;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 重试拦截器
 * <p>
 * 根据配置的重试策略对失败的请求进行自动重试。
 * 支持 ASYNC、FLOW、TASK 三种请求类型。
 * </p>
 */
public class RetryInterceptor implements Interceptor {

    private final RetryStrategy strategy;

    private RetryInterceptor(RetryStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletionStage<?> intercept(Chain chain) {
        return doRetry(chain, 0);
    }

    /**
     * 执行重试逻辑
     *
     * @param chain   拦截链
     * @param attempt 当前尝试次数（从0开始）
     * @return 处理结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompletionStage<?> doRetry(Chain chain, int attempt) {
        return chain.proceed()
                .exceptionallyCompose(ex -> {
                    // 计算是否重试及延迟时间
                    final var delay = strategy.decide(attempt, ex);

                    // null 表示不重试，直接返回失败
                    if (delay == null) {
                        return CompletableFuture.failedFuture(ex);
                    }

                    // 延迟时间为 0 或负数，表示立即重试
                    if (delay.isZero() || delay.isNegative()) {
                        return (CompletionStage) doRetry(chain, attempt + 1);
                    }

                    // 延迟后重试
                    return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS))
                            .thenCompose(v -> (CompletionStage) doRetry(chain, attempt + 1));
                });
    }

    /**
     * 构建器
     */
    public static class Builder {

        private RetryStrategy strategy = RetryStrategies.fixedDelay(Duration.ofSeconds(1), 3);

        /**
         * 设置重试策略
         *
         * @param strategy 重试策略
         * @return this
         */
        public Builder strategy(RetryStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * 构建重试拦截器
         *
         * @return 重试拦截器实例
         */
        public RetryInterceptor build() {
            return new RetryInterceptor(strategy);
        }

    }

}
