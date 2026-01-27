package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class ExchangeConnector<T, R> {

    private static final int ATTEMPT_BEGIN = 1;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final DashscopeClient client;
    private final Model model;
    private final Exchange.Codec<T, R> codec;
    private final Supplier<? extends Exchange.Handler<T, R>> handlerFactory;
    private final ReconnectStrategy reconnectStrategy;

    private final String _toString;
    private final Timer timer = new Timer();
    private volatile boolean shutdown = false;

    public ExchangeConnector(Builder<T, R, ?, ?> builder) {
        requireNonBlankString(builder.name, "name must not be blank!");
        requireNonNull(builder.client, "client must not be null!");
        requireNonNull(builder.model, "model must not be null!");
        requireNonNull(builder.codec, "codec must not be null!");
        requireNonNull(builder.handlerFactory, "handlerFactory must not be null!");
        requireNonNull(builder.reconnectStrategy, "reconnectStrategy must not be null!");
        this.name = builder.name;
        this.client = builder.client;
        this.model = builder.model;
        this.codec = builder.codec;
        this.handlerFactory = builder.handlerFactory;
        this.reconnectStrategy = builder.reconnectStrategy;
        this._toString = "dashscope4j-client://exchange/connector/%s@%s".formatted(name, System.identityHashCode(this));
    }

    @Override
    public String toString() {
        return _toString;
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
    public CompletionStage<Void> connect() {
        if (isShutdown()) {
            throw new IllegalStateException("Connector is shutdown!");
        }
        return reconnect(ATTEMPT_BEGIN, reconnectStrategy);
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

        return client.newExchange(model, codec, handlerFactory.get())
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



    public static abstract class Builder<T, R, C extends ExchangeConnector<T, R>, B extends Builder<T, R, C, B>> implements Buildable<C, B> {

        private String name = "normal";
        private DashscopeClient client;
        private Model model;
        private Exchange.Codec<T, R> codec;
        private Supplier<? extends Exchange.Handler<T, R>> handlerFactory;
        private ReconnectStrategy reconnectStrategy;

        protected Builder() {

        }

        protected Builder(ExchangeConnector<T, R> connector) {
            this.name = connector.name;
            this.client = connector.client;
            this.model = connector.model;
            this.codec = connector.codec;
            this.handlerFactory = connector.handlerFactory;
            this.reconnectStrategy = connector.reconnectStrategy;
        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B client(DashscopeClient client) {
            this.client = client;
            return self();
        }

        public B model(Model model) {
            this.model = model;
            return self();
        }

        public B codec(Exchange.Codec<T, R> codec) {
            this.codec = codec;
            return self();
        }

        public B handlerFactory(Supplier<? extends Exchange.Handler<T, R>> handlerFactory) {
            this.handlerFactory = handlerFactory;
            return self();
        }

        public B reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = reconnectStrategy;
            return self();
        }


    }

}
