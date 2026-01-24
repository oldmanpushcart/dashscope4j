package io.github.oldmanpushcart.dashscope4j.client.internal.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeErrorException;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeSessionUpdatedServerEvent;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

class SessionHandshakeHandler implements OmniRealtimeExchange.Handler {

    private final OmniRealtimeSession session;
    private final OmniRealtimeExchange.Handler delegate;

    private final AtomicReference<State> stateRef = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Exchange<OmniRealtimeClientEvent> exchange;

    public SessionHandshakeHandler(OmniRealtimeSession session, OmniRealtimeExchange.Handler delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
        this.exchange = exchange;
    }

    private void changeState(State expect, State update) {
        if (!stateRef.compareAndSet(expect, update)) {
            throw new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(
                    expect,
                    stateRef.get()
            ));
        }
    }

    private boolean isResponseCancelError(OmniRealtimeErrorServerEvent.Error error) {
        return null != error
                && "invalid_request_error".equals(error.type())
                && "Conversation has none active response".equals(error.message());
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

        if (data instanceof OmniRealtimeErrorServerEvent event) {
            final var error = event.error();

            /*
             * 如果是响应取消错误，则忽略
             * 因为会存在竞争的情况，即响应完成和响应取消同时发生。这种情况的错误是可被允许。
             */
            if (isResponseCancelError(error)) {
                return CompletableFuture.completedStage(null);
            }

            final var cause = new OmniRealtimeErrorException(error.code(), error.message());
            return CompletableFuture.failedStage(cause);
        }

        final var state = stateRef.get();
        return switch (state) {
            case AWAITING_SESSION_CREATED -> {
                if (data instanceof OmniRealtimeSessionCreatedServerEvent) {
                    changeState(state, State.AWAITING_SESSION_CONFIRMED);
                    final var event = new OmniRealtimeSessionUpdateClientEvent(genUUID22(), session);
                    yield exchange.send(event);
                } else {
                    final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            data.type()
                    ));
                    yield CompletableFuture.failedStage(cause);
                }
            }
            case AWAITING_SESSION_CONFIRMED -> {
                if (data instanceof OmniRealtimeSessionUpdatedServerEvent event) {
                    changeState(state, State.HANDSHAKE_COMPLETED);
                    final var session = event.session();
                    final var omniRealtimeExchange = new OmniRealtimeExchangeImpl(exchange, session);
                    delegate.onOpen(omniRealtimeExchange);
                    yield CompletableFuture.completedStage(null);
                } else {
                    final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.updated",
                            data.type()
                    ));
                    yield CompletableFuture.failedStage(cause);
                }
            }
            case HANDSHAKE_COMPLETED -> delegate.onData(data);
        };
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        final var cause = new UnsupportedOperationException("Binary data is not supported");
        return CompletableFuture.failedStage(cause);
    }

    @Override
    public void onClosed(Throwable ex) {
        delegate.onClosed(ex);
    }

    private enum State {

        /**
         * 等待 session.created
         */
        AWAITING_SESSION_CREATED,

        /**
         * 确认 session.updated
         */
        AWAITING_SESSION_CONFIRMED,

        /**
         * 握手完成
         */
        HANDSHAKE_COMPLETED,

    }

    private static class OmniRealtimeExchangeImpl extends Exchange.Proxy<OmniRealtimeClientEvent> implements OmniRealtimeExchange {

        private final OmniRealtimeSession session;

        /**
         * 构造一个代理实例，包装给定的原始 {@link Exchange}。
         *
         * @param delegate 被代理的原始数据交换对象，不可为 {@code null}
         * @throws NullPointerException 如果 {@code origin} 为 {@code null}
         */
        protected OmniRealtimeExchangeImpl(Exchange<OmniRealtimeClientEvent> delegate, OmniRealtimeSession session) {
            super(delegate);
            this.session = session;
        }

        @Override
        public OmniRealtimeSession session() {
            return session;
        }

    }

}
