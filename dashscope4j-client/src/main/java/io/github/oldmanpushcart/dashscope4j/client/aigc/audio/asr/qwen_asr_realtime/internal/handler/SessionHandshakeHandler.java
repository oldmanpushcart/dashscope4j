package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.SessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.SessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.SessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class SessionHandshakeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private final QwenAsrRealtimeSession session;
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    private volatile Realtime.Emitter<ClientEvent> emitter;
    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_SESSION_CREATED);

    public SessionHandshakeHandler(QwenAsrRealtimeSession session, Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        this.emitter = emitter;
    }

    @Override
    public CompletionStage<Void> onData(ServerEvent output) {

        if (output instanceof ErrorServerEvent event) {
            final var error = event.error();
            throw new IllegalStateException("Server error! code=%s;desc=%s".formatted(
                    error.code(),
                    error.message()
            ));
        }

        final var s = state.get();
        return switch (s) {
            case AWAITING_SESSION_CREATED -> {
                if (!(output instanceof SessionCreatedServerEvent)) {
                    throw new IllegalStateException("Expect session.created event, but was: " + output.type());
                }
                if (!state.compareAndSet(s, State.AWAITING_SESSION_CONFIRMED)) {
                    throw new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(s, state.get()));
                }
                final var sessionUpdateEvent = new SessionUpdateClientEvent(genUUID22(), session);
                yield emitter.data(sessionUpdateEvent);
            }
            case AWAITING_SESSION_CONFIRMED -> {
                if (!(output instanceof SessionUpdatedServerEvent event)) {
                    throw new IllegalStateException("Expect session.updated event, but was: " + output.type());
                }
                if (!state.compareAndSet(s, State.HANDSHAKE_COMPLETED)) {
                    throw new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(s, state.get()));
                }

                final var session = event.session();
                final var newSession = QwenAsrRealtimeSession.newBuilder(session)
                        .model(session.model())
                        .build();
                final var qwenAsrRealtimeEmitter = new QwenAsrRealtimeEmitterImpl(emitter, newSession);
                delegate.onOpen(qwenAsrRealtimeEmitter);
                yield CompletableFuture.completedFuture(null);
            }
            case HANDSHAKE_COMPLETED -> delegate.onData(output);
        };
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return delegate.onBinary(buffer);
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

    private static class QwenAsrRealtimeEmitterImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenAsrRealtimeEmitter {

        private final QwenAsrRealtimeSession session;

        public QwenAsrRealtimeEmitterImpl(Realtime.Emitter<ClientEvent> delegate, QwenAsrRealtimeSession session) {
            super(delegate);
            this.session = session;
        }

        @Override
        public QwenAsrRealtimeSession session() {
            return session;
        }

    }

}
