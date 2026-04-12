package io.github.oldmanpushcart.dashscope4j.client.util.retry;

import java.time.Duration;

/**
 * 重试策略：根据尝试次数和异常决定延迟时间
 */
@FunctionalInterface
public interface RetryStrategy {

    /**
     * 根据尝试次数和异常决定延迟时间
     *
     * @param attempt 尝试次数（从0开始，0表示首次调用）
     * @param ex      异常
     * @return <p>
     * - {@code null}：停止重试/重连；
     * - {@code Duration <= 0}：立即重试/重连；
     * - {@code Duration > 0}：延迟重试/重连
     */
    Duration decide(int attempt, Throwable ex);

}
