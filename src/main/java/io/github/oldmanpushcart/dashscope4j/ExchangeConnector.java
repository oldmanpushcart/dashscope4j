package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.util.Buildable;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import static java.util.Objects.nonNull;

/**
 * 数据交换连接器
 * <p>
 * 当前的数据交换{@link Exchange}都是单次连接，如果有处理连接失败时候的自动重连，可以使用这个类来完成。
 * </p>
 *
 * @since 3.1.0
 */
public interface ExchangeConnector {

    /**
     * @return 是否已连接
     */
    boolean isConnected();

    /**
     * 连接网络
     *
     * @return 连接通知
     */
    CompletionStage<?> connect();

    /**
     * 断开连接
     *
     * @return 断连通知
     */
    CompletionStage<?> disconnect();

    /**
     * 连接回调
     */
    interface Callback {

        /**
         * 连接建立后
         *
         * @param connector 同步器
         */
        default void afterConnectionEstablished(ExchangeConnector connector) {

        }

        /**
         * 连接断开后
         *
         * @param connector 同步器
         * @param cause     断开原因；
         *                  <li>如果是正常断开，则为null</li>
         *                  <li>如果是异常断开，则为对应的异常值</li>
         */
        default void afterConnectionLost(ExchangeConnector connector, Throwable cause) {

        }

        /**
         * 连接失败后
         *
         * @param connector 同步器
         * @param cause     连接失败原因
         */
        default void afterConnectFailed(ExchangeConnector connector, Throwable cause) {

        }

    }

    /**
     * 数据交换连接构建器
     *
     * @param <S> 连接器类型
     * @param <B> 构造器类型
     */
    interface Builder<S extends ExchangeConnector, B extends Builder<S, B>> extends Buildable<S, B> {

        /**
         * 设置回调集合
         *
         * @param callbacks 回调集合
         * @return this
         */
        B callbacks(Collection<? extends Callback> callbacks);

        /**
         * 添加回调
         *
         * @param callback 回调
         * @return this
         */
        B addCallback(Callback callback);

        /**
         * 添加回调集合
         *
         * @param callbacks 回调集合
         * @return this
         */
        B addCallbacks(Collection<? extends Callback> callbacks);

    }

    /**
     * 重连回调
     */
    class ReconnectCallback implements Callback {

        private static final int INIT_RETRIES = -1;
        private final Strategy strategy;
        private final AtomicInteger retriesRef = new AtomicInteger(INIT_RETRIES);

        public ReconnectCallback(Strategy strategy) {
            this.strategy = strategy;
        }

        @Override
        public void afterConnectionEstablished(ExchangeConnector connector) {
            retriesRef.set(INIT_RETRIES);
        }

        @Override
        public void afterConnectionLost(ExchangeConnector connector, Throwable cause) {
            reconnect(connector, cause);
        }

        @Override
        public void afterConnectFailed(ExchangeConnector connector, Throwable cause) {
            reconnect(connector, cause);
        }

        private void reconnect(ExchangeConnector connector, Throwable cause) {
            if (nonNull(cause)
                && nonNull(strategy)
                && strategy.evaluate(connector, retriesRef.incrementAndGet(), cause)) {
                connector.connect();
            }
        }

        /**
         * 创建重连回调
         *
         * @param strategy 重连策略
         * @return 重连回调
         */
        public static ReconnectCallback byStrategy(Strategy strategy) {
            return new ReconnectCallback(strategy);
        }


        /**
         * 重连策略
         */
        @FunctionalInterface
        public interface Strategy {

            /**
             * 执行策略
             * <p>
             * 评估是否应该尝试重新连接，并可能在此过程中执行必要的准备操作（比如：延迟）。
             * 若评估结果是继续重试，则返回true；否则返回false。
             * </p>
             *
             * @param connector 连接器
             * @param retries   当前重连次数
             * @param cause     当前连接失败的异常
             * @return 是否继续重试
             */
            boolean evaluate(ExchangeConnector connector, int retries, Throwable cause);

            /**
             * 与
             *
             * @param next 下一条策略
             * @return 策略组合
             */
            default Strategy and(Strategy next) {
                return (connector, retries, ex)
                        -> evaluate(connector, retries, ex)
                           && next.evaluate(connector, retries, ex);
            }

            /**
             * 或
             *
             * @param next 下一条策略
             * @return 策略组合
             */
            default Strategy or(Strategy next) {
                return (connector, retries, ex)
                        -> evaluate(connector, retries, ex)
                           || next.evaluate(connector, retries, ex);
            }

            /**
             * 非
             *
             * @return 策略组合
             */
            default Strategy negate() {
                return (connector, retries, ex) -> !evaluate(connector, retries, ex);
            }

        }

        /**
         * 默认策略集
         * <p>
         * 这里提供了常用的重连策略，若不满足可以自行实现{@link Strategy}接口。
         * </p>
         */
        public static class Strategies {

            /**
             * 退避策略
             *
             * @param maxRetries 最大退避次数；0：表示不退避；-1：表示无限退避；
             * @param intervalFn 退避间隔时间计算函数
             *                   <p>
             *                   {@code ( 当前退避次数 , 当前失败原因 ) = 本次退避间隔时间}
             *                   </p>
             * @return 重试策略
             */
            public static Strategy backoff(int maxRetries, BiFunction<Integer, Throwable, Duration> intervalFn) {
                final ReentrantLock lock = new ReentrantLock();
                final Condition condition = lock.newCondition();
                return (connection, retries, ex) -> {

                    /*
                     * 如果配置了最大退避次数：maxRetries >= 0，
                     * 则进行最大次数限制检查
                     */
                    if (maxRetries >= 0 && retries > maxRetries) {
                        return false;
                    }

                    // 退避算法计算本次退避时间
                    final Duration interval = Optional.ofNullable(intervalFn.apply(retries, ex)).orElse(Duration.ZERO);
                    lock.lock();
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        condition.await(interval.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    } finally {
                        lock.unlock();
                    }

                    return true;
                };
            }

        }

    }

}
