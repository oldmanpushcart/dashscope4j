package io.github.oldmanpushcart.dashscope4j.client.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * 支持自动重连的 Exchange 连接器。
 */
public class ExchangeConnector {

    private static final int ATTEMPT_BEGIN = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final Supplier<CompletionStage<? extends Exchange<?>>> factory;
    private final String _toString;
    private final Timer timer;

    private volatile boolean shutdown = false;

    public ExchangeConnector(Supplier<CompletionStage<? extends Exchange<?>>> factory) {
        this("normal", factory);
    }

    public ExchangeConnector(String name, Supplier<CompletionStage<? extends Exchange<?>>> factory) {
        this.name = name;
        this.factory = factory;
        this._toString = "dashscope4j-client://exchange/connector/%s@%s".formatted(name, System.identityHashCode(this));
        this.timer = new Timer("%s/timer".formatted(this), true);
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 获取连接器的名称。
     */
    public String name() {
        return name;
    }

    /**
     * 获取连接器是否已停止。
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * 停止连接器。
     */
    public synchronized void shutdown() {
        shutdown = true;
        timer.cancel();
    }

    /**
     * 启动连接（从第 1 次尝试开始）。
     */
    public CompletionStage<Void> connect(ReconnectStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null!");
        if (isShutdown()) {
            throw new IllegalStateException("Connector is shutdown!");
        }
        return reconnect(ATTEMPT_BEGIN, strategy);
    }

    /**
     * 执行第 {@code attempt} 次连接尝试。
     */
    private CompletionStage<Void> reconnect(int attempt, ReconnectStrategy strategy) {

        if (isShutdown()) {
            return CompletableFuture.failedStage(
                    new IllegalStateException("Connector was shutdown during connect at attempt %d!".formatted(attempt))
            );
        }

        return factory.get()
                .handle((exchange, connectEx) -> {

                    // 连接失败，尝试重连
                    if (null != connectEx) {
                        return scheduleRetry(attempt, connectEx, strategy);
                    }

                    // 连接成功，注册重连
                    exchange.closeFuture()
                            .whenComplete((v, closeEx) -> {
                                if (null == closeEx) {
                                    logger.debug("{} connection closed normally at attempt {}", this, attempt);
                                } else {
                                    logger.warn("{} connection closed unexpectedly at attempt {}", this, attempt, closeEx);
                                    scheduleRetry(ATTEMPT_BEGIN, closeEx, strategy);
                                }
                            });

                    return CompletableFuture.<Void>completedStage(null);

                })
                .thenCompose(f -> f);
    }

    /**
     * 根据策略决定是否重试，并执行相应动作。
     */
    private CompletionStage<Void> scheduleRetry(int attempt, Throwable cause, ReconnectStrategy strategy) {

        /*
         * 尝试执行策略，
         * 若策略执行失败视为拒绝重连
         */
        final Duration delay;
        try {
            delay = strategy.decide(attempt, cause);
        } catch (Throwable strategyEx) {
            logger.warn("{} strategy decide error at attempt {}", this, attempt, strategyEx);
            final var rejectedEx = new RejectedReconnectException("Reconnect rejected by strategy decide error!", cause);
            return CompletableFuture.failedFuture(rejectedEx);
        }

        // 策略拒绝重连
        if (delay == null) {
            logger.debug("{} retry rejected by strategy at attempt {}", this, attempt);
            final var rejectEx = new RejectedReconnectException("Reconnect rejected by strategy!", cause);
            return CompletableFuture.failedFuture(rejectEx);
        }

        // 延迟重试：使用 daemon Timer
        final var delayMs = Math.max(0, delay.toMillis());
        logger.debug("{} retry scheduling after {}ms at attempt {}", this, delayMs, attempt);
        final var retryF = new CompletableFuture<Void>();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    if (isShutdown()) {
                        retryF.completeExceptionally(
                                new IllegalStateException("Connector was shutdown during retry at attempt %d!".formatted(attempt))
                        );
                        return;
                    }
                    reconnect(attempt + 1, strategy)
                            .whenComplete((exchange, reconnectEx) -> {
                                if (reconnectEx != null) {
                                    retryF.completeExceptionally(reconnectEx);
                                } else {
                                    retryF.complete(null);
                                }
                            });
                } catch (Throwable reconnectEx) {
                    retryF.completeExceptionally(reconnectEx);
                }
            }

        }, delayMs);

        return retryF;

    }

    // —————————————— 公共接口 ——————————————

    /**
     * 表示重连被策略拒绝或策略执行失败。
     */
    public static class RejectedReconnectException extends RuntimeException {

        public RejectedReconnectException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * 重连策略：根据尝试次数和异常决定延迟时间。
     */
    @FunctionalInterface
    public interface ReconnectStrategy {

        /**
         * 根据尝试次数和异常决定延迟时间。
         *
         * @param attempt 尝试次数
         * @param ex      异常
         * @return <p>
         * - {@code null}：停止重连；
         * - {@code Duration <= 0}：立即重连；
         * - {@code Duration > 0}：延迟重连。
         */
        Duration decide(int attempt, Throwable ex);

    }


    /**
     * 重连策略工厂。
     */
    public interface ReconnectStrategies {

        /**
         * 永不重连。
         */
        static ReconnectStrategy never() {
            return (attempt, ex) -> null;
        }

        /**
         * 立即重连，并持续重连。
         */
        static ReconnectStrategy immediateForever() {
            return (attempt, ex) -> Duration.ZERO;
        }

        /**
         * 固定延迟重连。
         */
        static ReconnectStrategy fixedDelay(Duration delay) {
            return (attempt, ex) -> delay;
        }

        /**
         * 指数退避重连。
         */
        static ReconnectStrategy exponentialBackoff(Duration initialDelay, Duration maxDelay) {
            return (attempt, ex) -> {
                final var delay = initialDelay.multipliedBy(2).multipliedBy(attempt);
                return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
            };
        }
    }

}
