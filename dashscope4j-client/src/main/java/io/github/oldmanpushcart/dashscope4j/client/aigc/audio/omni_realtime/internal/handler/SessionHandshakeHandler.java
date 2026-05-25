package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeErrorException;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.SessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.SessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.SessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.client.util.UUIDUtils.genUUID22;

public class SessionHandshakeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private final OmniRealtimeSession session;
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Realtime.Emitter<ClientEvent> emitter;

    public SessionHandshakeHandler(OmniRealtimeSession session, Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        this.emitter = emitter;
    }

    private boolean isResponseCancelError(ErrorServerEvent.Error error) {
        return null != error
                && "invalid_request_error".equals(error.type())
                && "Conversation has none active response".equals(error.message());
    }

    @Override
    public void onData(ServerEvent output) {

        if (output instanceof ErrorServerEvent event) {
            final var error = event.error();

            /*
             * 如果是响应取消错误，则忽略
             * 因为会存在竞争的情况，即响应完成和响应取消同时发生。这种情况的错误是可被允许。
             */
            if (isResponseCancelError(error)) {
                return;
            }

            throw new OmniRealtimeErrorException(error.code(), error.message());
        }

        final var state = this.state.get();
        switch (state) {
            case AWAITING_SESSION_CREATED -> {
                if (output instanceof SessionCreatedServerEvent) {
                    if (!this.state.compareAndSet(state, State.AWAITING_SESSION_CONFIRMED)) {
                        throw new IllegalStateException("Expect %s state, but was: %s".formatted(
                                state,
                                this.state.get()
                        ));
                    }
                    emitter.data(new SessionUpdateClientEvent(genUUID22(), session));
                } else {
                    throw new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            output.type()
                    ));
                }
            }
            case AWAITING_SESSION_CONFIRMED -> {
                if (output instanceof SessionUpdatedServerEvent event) {
                    if (!this.state.compareAndSet(state, State.HANDSHAKE_COMPLETED)) {
                        throw new IllegalStateException("Expect %s state, but was: %s".formatted(
                                state,
                                this.state.get()
                        ));
                    }
                    final var session = event.session();
                    final var newSession = OmniRealtimeSession.newBuilder(session)
                            .model(session.model())
                            .build();
                    final var omniRealtimeEmitter = new OmniRealtimeEmitterImpl(emitter, newSession);
                    delegate.onOpen(omniRealtimeEmitter);
                } else {
                    throw new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.updated",
                            output.type()
                    ));
                }
            }
            case HANDSHAKE_COMPLETED -> delegate.onData(output);
        }
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Binary data is not supported");
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

    private static class OmniRealtimeEmitterImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements OmniRealtimeEmitter {

        private final OmniRealtimeSession session;

        /**
         * 构造一个代理实例，包装给定的原始 {@link Realtime.Emitter}。
         *
         * @param delegate 被代理的原始数据交换对象，不可为 {@code null}
         * @throws NullPointerException 如果 {@code origin} 为 {@code null}
         */
        protected OmniRealtimeEmitterImpl(Realtime.Emitter<ClientEvent> delegate, OmniRealtimeSession session) {
            super(delegate);
            this.session = session;
        }

        @Override
        public OmniRealtimeSession session() {
            return session;
        }

    }

}
