package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.SessionFinishClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.SessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.SessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.SessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class SessionHandshakeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_SESSION_FINISHED = "session.finished";

    private final QwenTtsRealtimeSession session;
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Realtime.Emitter<ClientEvent> emitter;

    public SessionHandshakeHandler(QwenTtsRealtimeSession session, Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        this.emitter = emitter;
    }

    private void changeState(State expect, State update) {
        if (!state.compareAndSet(expect, update)) {
            throw new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(
                    expect,
                    state.get()
            ));
        }
    }

    @Override
    public CompletionStage<Void> onData(ServerEvent output) {

        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        if (output instanceof ErrorServerEvent event) {
            final var error = event.error();
            final var cause = new IllegalStateException("Server error! code=%s;desc=%s".formatted(
                    error.code(),
                    error.message()
            ));
            return CompletableFuture.failedStage(cause);
        }

        final var s = state.get();
        return switch (s) {
            case AWAITING_SESSION_CREATED -> {
                if (output instanceof SessionCreatedServerEvent) {
                    changeState(s, State.AWAITING_SESSION_CONFIRMED);
                    final var event = new SessionUpdateClientEvent(genUUID22(), session);
                    yield emitter.data(event);
                } else {
                    final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            output.type()
                    ));
                    yield CompletableFuture.failedStage(cause);
                }
            }
            case AWAITING_SESSION_CONFIRMED -> {
                if (output instanceof SessionUpdatedServerEvent event) {
                    changeState(s, State.HANDSHAKE_COMPLETED);
                    final var session = event.session();
                    final var newSession = QwenTtsRealtimeSession.newBuilder(session)
                            .model(session.model())
                            .build();
                    final var qwenTtsRealtimeEmitter = new QwenTtsRealtimeEmitterImpl(newSession, emitter, futureMap);
                    delegate.onOpen(qwenTtsRealtimeEmitter);
                    yield CompletableFuture.completedStage(null);
                } else {
                    final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            output.type()
                    ));
                    yield CompletableFuture.failedStage(cause);
                }
            }
            case HANDSHAKE_COMPLETED -> delegate.onData(output);
        };
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        final var cause = new UnsupportedOperationException("Binary data is not supported");
        return CompletableFuture.failedStage(cause);
    }

    @Override
    public void onClosed(Throwable ex) {
        futureMap.forEach((type, future) -> {
            if (null != future) {
                if (null != ex) {
                    future.completeExceptionally(ex);
                } else {
                    future.cancel(true);
                }
            }
        });
        futureMap.clear();
        delegate.onClosed(ex);
    }

    private enum State {
        AWAITING_SESSION_CREATED,
        AWAITING_SESSION_CONFIRMED,
        HANDSHAKE_COMPLETED;
    }

    private static final class QwenTtsRealtimeEmitterImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenTtsRealtimeEmitter {

        private final QwenTtsRealtimeSession session;
        private final Map<String, CompletableFuture<?>> futureMap;


        private QwenTtsRealtimeEmitterImpl(
                QwenTtsRealtimeSession session,
                Realtime.Emitter<ClientEvent> delegate,
                Map<String, CompletableFuture<?>> futureMap
        ) {
            super(delegate);
            this.session = session;
            this.futureMap = futureMap;
        }

        @Override
        public CompletionStage<Void> closing() {
            final var finishF = new CompletableFuture<Void>();
            if (futureMap.putIfAbsent(KEY_SESSION_FINISHED, finishF) != null) {
                throw new IllegalStateException("Exists finish running.");
            }
            final var event = new SessionFinishClientEvent(genUUID22());
            return data(event)
                    .thenCompose(unused -> finishF)
                    .whenComplete((unused, ex) -> futureMap.remove(KEY_SESSION_FINISHED))
                    .thenCompose(unused -> super.closing());
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return session;
        }

    }

}
