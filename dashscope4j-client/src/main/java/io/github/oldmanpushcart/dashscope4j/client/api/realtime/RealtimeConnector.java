package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * 实时连接器
 * <p>
 * 实时交互是通过长连接维持双工通讯，这里构建一个连接工具，方便实现短线重连等长连接维持策略
 * </p>
 */
public class RealtimeConnector {

    private static final int ATTEMPT_BEGIN = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<CompletionStage<? extends Realtime.Connection>> connectionFactory;
    private final ReconnectStrategy reconnectStrategy;

    private final String _toString;
    private final Timer timer = new Timer();
    private volatile boolean shutdown = false;

    private RealtimeConnector(Builder builder) {
        requireNonNull(builder.connectionFactory, "connectionFactory must not be null!");
        requireNonNull(builder.reconnectStrategy, "reconnectStrategy must not be null!");
        this.connectionFactory = builder.connectionFactory;
        this.reconnectStrategy = builder.reconnectStrategy;
        this._toString = "dashscope4j-client://exchange/connector/%s".formatted(System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 是否已停止
     */
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * 停止
     */
    public synchronized void shutdown() {
        shutdown = true;
        timer.cancel();
    }

    /**
     * 启动连接（从第 1 次尝试开始）
     */
    public CompletionStage<Void> connect() {
        if (isShutdown()) {
            throw new IllegalStateException("Connector is shutdown!");
        }
        return reconnect(ATTEMPT_BEGIN, reconnectStrategy);
    }

    /**
     * 执行第 {@code attempt} 次连接尝试
     */
    private CompletionStage<Void> reconnect(int attempt, ReconnectStrategy strategy) {

        if (isShutdown()) {
            return CompletableFuture.failedStage(
                    new IllegalStateException("Connector was shutdown during connect at attempt %d!".formatted(attempt))
            );
        }

        return connectionFactory.get()
                .handle((connection, connectEx) -> {

                    // 连接失败，尝试重连
                    if (null != connectEx) {
                        return scheduleRetry(attempt, connectEx, strategy);
                    }

                    // 连接成功，注册重连
                    connection.closeFuture()
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
     * 根据策略决定是否重试，并执行相应动作
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
        final var completeF = new CompletableFuture<Void>();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    if (isShutdown()) {
                        completeF.completeExceptionally(
                                new IllegalStateException("Connector was shutdown during retry at attempt %d!".formatted(attempt))
                        );
                        return;
                    }
                    reconnect(attempt + 1, strategy)
                            .whenComplete((exchange, reconnectEx) -> {
                                if (reconnectEx != null) {
                                    completeF.completeExceptionally(reconnectEx);
                                } else {
                                    completeF.complete(null);
                                }
                            });
                } catch (Throwable reconnectEx) {
                    completeF.completeExceptionally(reconnectEx);
                }
            }

        }, delayMs);

        return completeF;

    }

    // —————————————— 公共接口 ——————————————

    /**
     * 表示重连被策略拒绝或策略执行失败
     */
    public static class RejectedReconnectException extends RuntimeException {

        public RejectedReconnectException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * 重连策略：根据尝试次数和异常决定延迟时间
     */
    @FunctionalInterface
    public interface ReconnectStrategy {

        /**
         * 根据尝试次数和异常决定延迟时间
         *
         * @param attempt 尝试次数
         * @param ex      异常
         * @return <p>
         * - {@code null}：停止重连；
         * - {@code Duration <= 0}：立即重连；
         * - {@code Duration > 0}：延迟重连
         */
        Duration decide(int attempt, Throwable ex);

    }

    /**
     * 重连策略工厂
     */
    public interface ReconnectStrategies {

        /**
         * 永不重连
         */
        static ReconnectStrategy never() {
            return (attempt, ex) -> null;
        }

        /**
         * 立即重连，并持续重连
         */
        static ReconnectStrategy immediateForever() {
            return (attempt, ex) -> Duration.ZERO;
        }

        /**
         * 固定延迟重连
         */
        static ReconnectStrategy fixedDelay(Duration delay) {
            return (attempt, ex) -> delay;
        }

        /**
         * 指数退避重连
         */
        static ReconnectStrategy exponentialBackoff(Duration initialDelay, Duration maxDelay) {
            return (attempt, ex) -> {
                final var delay = initialDelay.multipliedBy(2).multipliedBy(attempt);
                return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
            };
        }
    }

    /**
     * @return 创建连接器构造器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 连接器构造器
     */
    public static class Builder implements Buildable<RealtimeConnector, Builder> {

        private Supplier<CompletionStage<? extends Realtime.Connection>> connectionFactory;
        private ReconnectStrategy reconnectStrategy;

        /**
         * 设置连接工厂
         *
         * @param connectionFactory 连接工厂
         * @return this
         */
        public Builder connectionFactory(Supplier<CompletionStage<? extends Realtime.Connection>> connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        /**
         * 设置重连策略
         *
         * @param reconnectStrategy 重连策略
         * @return this
         */
        public Builder reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = reconnectStrategy;
            return this;
        }


        @Override
        public RealtimeConnector build() {
            return new RealtimeConnector(this);
        }

    }

}
