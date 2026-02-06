package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeSessionFinishClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class SessionHandshakeHandler implements Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> {

    private static final String KEY_SESSION_FINISHED = "session.finished";

    private final QwenTtsRealtimeSession session;
    private final Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> delegate;

    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_SESSION_CREATED);
    private volatile Realtime.Emitter<QwenTtsRealtimeClientEvent> emitter;

    public SessionHandshakeHandler(QwenTtsRealtimeSession session, Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<QwenTtsRealtimeClientEvent> emitter) {
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
    public CompletionStage<Void> onData(QwenTtsRealtimeServerEvent output) {

        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        if (output instanceof QwenTtsRealtimeErrorServerEvent event) {
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
                if (output instanceof QwenTtsRealtimeSessionCreatedServerEvent) {
                    changeState(s, State.AWAITING_SESSION_CONFIRMED);
                    final var event = new QwenTtsRealtimeSessionUpdateClientEvent(genUUID22(), session);
                    yield emitter.emit(event);
                } else {
                    final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            output.type()
                    ));
                    yield CompletableFuture.failedStage(cause);
                }
            }
            case AWAITING_SESSION_CONFIRMED -> {
                if (output instanceof QwenTtsRealtimeSessionUpdatedServerEvent event) {
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
                future.completeExceptionally(ex);
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

    private static final class QwenTtsRealtimeEmitterImpl implements QwenTtsRealtimeEmitter {

        private final QwenTtsRealtimeSession session;
        private final Realtime.Emitter<QwenTtsRealtimeClientEvent> delegate;
        private final Map<String, CompletableFuture<?>> futureMap;


        private QwenTtsRealtimeEmitterImpl(
                QwenTtsRealtimeSession session,
                Realtime.Emitter<QwenTtsRealtimeClientEvent> delegate,
                Map<String, CompletableFuture<?>> futureMap
        ) {
            this.session = session;
            this.delegate = delegate;
            this.futureMap = futureMap;
        }

        private void register(String key, CompletableFuture<?> future) {
            if (futureMap.putIfAbsent(key, future) != null) {
                throw new IllegalStateException("Key: %s already registered!".formatted(key));
            }
        }

        private void unregister(String key, Throwable ex) {
            final var future = futureMap.remove(key);
            if (null != future) {
                if (null != ex) {
                    future.completeExceptionally(ex);
                } else {
                    future.complete(null);
                }
            }
        }

        @Override
        public CompletionStage<Void> emit(QwenTtsRealtimeClientEvent input) {
            return delegate.emit(input);
        }

        @Override
        public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
            return delegate.emitBinary(buffer);
        }

        @Override
        public CompletionStage<Void> emitClose() {
            final var finishF = new CompletableFuture<Void>();
            register(KEY_SESSION_FINISHED, finishF);
            final var event = new QwenTtsRealtimeSessionFinishClientEvent(genUUID22());
            return delegate.emit(event)
                    .thenCompose(unused -> finishF)
                    .whenComplete((unused, ex) -> unregister(KEY_SESSION_FINISHED, ex))
                    .thenCompose(unused-> delegate.emitClose());
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

        @Override
        public QwenTtsRealtimeSession session() {
            return session;
        }

        public Realtime.Emitter<QwenTtsRealtimeClientEvent> delegate() {
            return delegate;
        }

    }

}
