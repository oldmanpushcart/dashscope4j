package io.github.oldmanpushcart.dashscope4j.client.util.retry;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * 重试策略工厂
 */
public final class RetryStrategies {

    private RetryStrategies() {
        // 工具类，禁止实例化
    }

    /**
     * 永不重试
     *
     * @return 永不重试的策略
     */
    public static RetryStrategy never() {
        return (attempt, ex) -> null;
    }

    /**
     * 立即重试，并持续重试（无限制）
     *
     * @return 立即且持续重试的策略
     */
    public static RetryStrategy immediateForever() {
        return (attempt, ex) -> Duration.ZERO;
    }

    /**
     * 固定延迟重试（无次数限制）
     *
     * @param delay 固定延迟时间
     * @return 固定延迟重试策略
     */
    public static RetryStrategy fixedDelay(Duration delay) {
        return (attempt, ex) -> delay;
    }

    /**
     * 固定延迟重试（带次数限制）
     *
     * @param delay      固定延迟时间
     * @param maxRetries 最大重试次数
     * @return 固定延迟重试策略
     */
    public static RetryStrategy fixedDelay(Duration delay, int maxRetries) {
        return (attempt, ex) -> {
            if (attempt >= maxRetries || ex == null) {
                return null;
            }
            return delay;
        };
    }

    /**
     * 指数退避重试（无次数限制）
     *
     * @param initialDelay 初始延迟时间
     * @param maxDelay     最大延迟时间
     * @return 指数退避重试策略
     */
    public static RetryStrategy exponentialBackoff(Duration initialDelay, Duration maxDelay) {
        return (attempt, ex) -> {
            final var delay = initialDelay.multipliedBy(2).multipliedBy(attempt);
            return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
        };
    }

    /**
     * 指数退避重试（带次数限制）
     *
     * @param baseDelay  基础延迟时间
     * @param maxDelay   最大延迟时间
     * @param maxRetries 最大重试次数
     * @return 指数退避重试策略
     */
    public static RetryStrategy exponentialBackoff(Duration baseDelay, Duration maxDelay, int maxRetries) {
        return (attempt, ex) -> {
            if (attempt >= maxRetries || ex == null) {
                return null;
            }
            final var delay = baseDelay.multipliedBy((long) Math.pow(2, attempt));
            return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
        };
    }

    /**
     * 条件重试
     *
     * @param delay      延迟时间
     * @param maxRetries 最大重试次数
     * @param predicate  重试条件判断
     * @return 条件重试策略
     */
    public static RetryStrategy conditional(Duration delay, int maxRetries, Predicate<Throwable> predicate) {
        return (attempt, ex) -> {
            if (attempt >= maxRetries || ex == null || !predicate.test(ex)) {
                return null;
            }
            return delay;
        };
    }

}
