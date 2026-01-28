package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.Realtime;
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

class SessionHandshakeHandler implements Realtime.Handler<OmniRealtimeServerEvent, OmniRealtimeClientEvent> {

    private final OmniRealtimeSession session;
    private final Realtime.Handler<OmniRealtimeServerEvent, OmniRealtimeClientEvent> delegate;

    private final AtomicReference<State> stateRef = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Realtime.Emitter<OmniRealtimeClientEvent> emitter;

    public SessionHandshakeHandler(OmniRealtimeSession session, Realtime.Handler<OmniRealtimeServerEvent, OmniRealtimeClientEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<OmniRealtimeClientEvent> emitter) {
        this.emitter = emitter;
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
                    yield emitter.emit(event);
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
                    final var omniRealtimeExchange = new OmniRealtimeEmitterImpl(emitter, session);
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

    private static class OmniRealtimeEmitterImpl implements OmniRealtimeEmitter {

        private final Realtime.Emitter<OmniRealtimeClientEvent> delegate;
        private final OmniRealtimeSession session;

        /**
         * 构造一个代理实例，包装给定的原始 {@link Exchange}。
         *
         * @param delegate 被代理的原始数据交换对象，不可为 {@code null}
         * @throws NullPointerException 如果 {@code origin} 为 {@code null}
         */
        protected OmniRealtimeEmitterImpl(Realtime.Emitter<OmniRealtimeClientEvent> delegate, OmniRealtimeSession session) {
            this.delegate = delegate;
            this.session = session;
        }

        @Override
        public OmniRealtimeSession session() {
            return session;
        }

        @Override
        public CompletionStage<Void> emit(OmniRealtimeClientEvent output) {
            return delegate.emit(output);
        }

        @Override
        public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
            return delegate.emitBinary(buffer);
        }

        @Override
        public CompletionStage<Void> emitClose() {
            return delegate.emitClose();
        }

        @Override
        public CompletionStage<Void> emitClose(Throwable ex) {
            return delegate.emitClose(ex);
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return delegate.closeFuture();
        }

    }

}
