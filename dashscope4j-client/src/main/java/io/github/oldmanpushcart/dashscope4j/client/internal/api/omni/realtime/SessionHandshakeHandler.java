package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeErrorException;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

class SessionHandshakeHandler implements OmniRealtimeExchange.Handler {

    private final Parameters parameters;
    private final OmniRealtimeExchange.Handler delegate;

    private final AtomicReference<State> stateRef = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Exchange<OmniRealtimeClientEvent> exchange;

    public SessionHandshakeHandler(Parameters parameters, OmniRealtimeExchange.Handler delegate) {
        this.parameters = parameters;
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

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

        if (data instanceof OmniRealtimeErrorServerEvent event) {
            final var error = event.error();
            final var cause = new OmniRealtimeErrorException(error.code(), error.message());
            return CompletableFuture.failedStage(cause);
        }

        final var state = stateRef.get();
        return switch (state) {
            case AWAITING_SESSION_CREATED -> {
                if (data instanceof OmniRealtimeSessionCreatedServerEvent) {
                    changeState(state, State.AWAITING_SESSION_CONFIRMED);
                    final var session = new OmniRealtimeSession(parameters);
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
                if (data instanceof OmniRealtimeSessionUpdatedServerEvent) {
                    changeState(state, State.HANDSHAKE_COMPLETED);
                    delegate.onOpen(exchange);
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

}
