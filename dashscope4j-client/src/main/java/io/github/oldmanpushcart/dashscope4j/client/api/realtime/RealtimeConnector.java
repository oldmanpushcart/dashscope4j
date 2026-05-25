package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategy;
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

    private static final int ATTEMPT_BEGIN = 0;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Supplier<CompletionStage<? extends Realtime.Connection>> connectionFactory;
    private final RetryStrategy retryStrategy;

    private final String _toString;
    private final Timer timer = new Timer();
    private volatile boolean shutdown = false;

    private RealtimeConnector(Builder builder) {
        requireNonNull(builder.connectionFactory, "connectionFactory must not be null!");
        requireNonNull(builder.retryStrategy, "retryStrategy must not be null!");
        this.connectionFactory = builder.connectionFactory;
        this.retryStrategy = builder.retryStrategy;
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
     * 启动连接（从第 0 次尝试开始）
     */
    public CompletionStage<Void> connect() {
        if (isShutdown()) {
            throw new IllegalStateException("Connector is shutdown!");
        }
        return reconnect(ATTEMPT_BEGIN, retryStrategy);
    }

    /**
     * 执行第 {@code attempt} 次连接尝试
     */
    private CompletionStage<Void> reconnect(int attempt, RetryStrategy strategy) {

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
    private CompletionStage<Void> scheduleRetry(int attempt, Throwable cause, RetryStrategy strategy) {

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
                            .whenComplete((v, reconnectEx) -> {
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

    /**
     * 表示重连被策略拒绝或策略执行失败
     */
    public static class RejectedReconnectException extends RuntimeException {

        public RejectedReconnectException(String message, Throwable cause) {
            super(message, cause);
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
        private RetryStrategy retryStrategy;

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
         * 设置重试策略
         *
         * @param retryStrategy 重试策略
         * @return this
         */
        public Builder retryStrategy(RetryStrategy retryStrategy) {
            this.retryStrategy = retryStrategy;
            return this;
        }


        @Override
        public RealtimeConnector build() {
            return new RealtimeConnector(this);
        }

    }

}
