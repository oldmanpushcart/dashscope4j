package io.github.oldmanpushcart.dashscope4j.internal;

import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.ExchangeConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CompletableFutureUtils.*;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;

@Slf4j
public abstract class AbstractExchangeConnector implements ExchangeConnector {

    private final Callback callback;
    private final AtomicReference<State> stateRef = new AtomicReference<>(State.DISCONNECTED);
    private volatile Exchange<?> exchange;

    public AbstractExchangeConnector(Builder<?, ?> builder) {
        this.callback = builder.callbacks.stream()
                .map(SafelyCallback::new)
                .collect(collectingAndThen(Collectors.toList(), GroupCallback::new));
    }

    private void updateState(State expect, State value) {
        final State actual = stateRef.get();
        if (actual != expect || !stateRef.compareAndSet(expect, value)) {
            throw new IllegalStateException(String.format("Update state error! expect: %s, actual: %s!",
                    expect,
                    actual
            ));
        }
    }

    @Override
    public boolean isConnected() {
        return stateRef.get() == State.CONNECTED;
    }

    @Override
    public CompletionStage<?> connect() {

        // 锁定为连接中
        updateState(State.DISCONNECTED, State.CONNECTING);
        log.trace("{} connecting...", this);

        // 开始连接
        return doConnect()

                .handle((exchange, connectEx) -> {

                    // 处理连接失败
                    if (nonNull(connectEx)) {
                        updateState(State.CONNECTING, State.DISCONNECTED);
                        log.debug("{} connect failed!", this, connectEx);
                        callback.afterConnectFailed(this, unwrapEx(connectEx));
                        return failedStage(connectEx);
                    }

                    // 处理连接成功
                    else {
                        updateState(State.CONNECTING, State.CONNECTED);
                        log.debug("{} connect succeeded! exchange={}", this, exchange.uuid());

                        this.exchange = exchange;

                        // 挂载 Callback
                        callback.afterConnectionEstablished(this);
                        exchange.closeStage()
                                .whenComplete((v, ex) -> {
                                    if (nonNull(ex)) {
                                        log.debug("{} connection lost by error! exchange={}", this, exchange.uuid(), ex);
                                    } else {
                                        log.debug("{} connection lost by closed! exchange={}", this, exchange.uuid());
                                    }
                                    updateState(State.CONNECTED, State.DISCONNECTED);
                                    callback.afterConnectionLost(this, unwrapEx(ex));
                                });
                        return completedStage(exchange);

                    }

                })
                .thenCompose(v -> v);
    }

    abstract protected CompletionStage<Exchange<?>> doConnect();

    @Override
    public CompletionStage<?> disconnect() {

        // 锁定为断开中
        updateState(State.CONNECTED, State.DISCONNECTING);
        log.debug("{} disconnecting... exchange={};", this, exchange.uuid());

        // 开始断开
        return _disconnect()

                // 处理断开
                .handle((v, ex) -> {
                    if (nonNull(ex)) {
                        updateState(State.DISCONNECTING, State.DISCONNECTED);
                        log.debug("{} disconnect failed! exchange={};", this, exchange.uuid(), ex);
                        return failedStage(ex);
                    } else {
                        updateState(State.DISCONNECTING, State.DISCONNECTED);
                        log.debug("{} disconnect succeeded! exchange={};", this, exchange.uuid());
                        return CompletableFuture.completedFuture(v);
                    }
                })
                .thenCompose(v -> v);
    }

    private CompletionStage<?> _disconnect() {
        return closeForce(exchange)
                .closeStage()
                .exceptionally(ex -> null);
    }

    private static Exchange<?> closeForce(Exchange<?> exchange) {
        if (!exchange.isClosed() && !exchange.closing()) {
            exchange.abort();
        }
        return exchange;
    }

    /**
     * 连接状态
     */
    private enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    /**
     * 安全调用回调
     */
    private class SafelyCallback implements Callback {

        private final Callback delegate;

        private SafelyCallback(Callback delegate) {
            this.delegate = delegate;
        }

        private void safely(Runnable runnable) {
            try {
                runnable.run();
            } catch (Throwable t) {
                log.error("{} notify occur error!", AbstractExchangeConnector.this, t);
            }
        }

        @Override
        public void afterConnectionEstablished(ExchangeConnector connector) {
            safely(() -> delegate.afterConnectionEstablished(connector));
        }

        @Override
        public void afterConnectionLost(ExchangeConnector connector, Throwable cause) {
            safely(() -> delegate.afterConnectionLost(connector, cause));
        }

        @Override
        public void afterConnectFailed(ExchangeConnector connector, Throwable cause) {
            safely(() -> delegate.afterConnectFailed(connector, cause));
        }

    }

    /**
     * 组合回调
     */
    @Slf4j
    private static class GroupCallback implements Callback {

        private final List<Callback> callbacks = new ArrayList<>();

        public GroupCallback(Collection<? extends Callback> callbacks) {
            requireNonNull(callbacks);
            this.callbacks.addAll(callbacks);
        }

        @Override
        public void afterConnectionEstablished(ExchangeConnector connector) {
            callbacks.forEach(callback -> callback.afterConnectionEstablished(connector));
        }

        @Override
        public void afterConnectionLost(ExchangeConnector connector, Throwable cause) {
            callbacks.forEach(callback -> callback.afterConnectionLost(connector, cause));
        }

        @Override
        public void afterConnectFailed(ExchangeConnector connector, Throwable cause) {
            callbacks.forEach(callback -> callback.afterConnectFailed(connector, cause));
        }

    }

    public static abstract class Builder<S extends ExchangeConnector, B extends ExchangeConnector.Builder<S, B>> implements ExchangeConnector.Builder<S, B> {

        private final List<Callback> callbacks = new ArrayList<>();

        @Override
        public B callbacks(Collection<? extends Callback> callbacks) {
            requireNonNull(callbacks);
            this.callbacks.clear();
            this.callbacks.addAll(callbacks);
            return self();
        }

        @Override
        public B addCallback(Callback callback) {
            requireNonNull(callback);
            this.callbacks.add(callback);
            return self();
        }

        @Override
        public B addCallbacks(Collection<? extends Callback> callbacks) {
            requireNonNull(callbacks);
            this.callbacks.addAll(callbacks);
            return self();
        }

    }

}
