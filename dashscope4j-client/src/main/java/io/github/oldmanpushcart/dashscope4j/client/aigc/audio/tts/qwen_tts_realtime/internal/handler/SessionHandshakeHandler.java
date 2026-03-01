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

    @Override
    public void onData(ServerEvent output) {

        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        if (output instanceof ErrorServerEvent event) {
            final var error = event.error();
            throw new IllegalStateException("Server error! code=%s;desc=%s".formatted(
                    error.code(),
                    error.message()
            ));
        }

        final var s = state.get();
        switch (s) {
            case AWAITING_SESSION_CREATED -> {
                if (output instanceof SessionCreatedServerEvent) {
                    if (!state.compareAndSet(s, State.AWAITING_SESSION_CONFIRMED)) {
                        throw new IllegalStateException("Expect %s state, but was: %s".formatted(
                                s,
                                state.get()
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
                    if (!state.compareAndSet(s, State.HANDSHAKE_COMPLETED)) {
                        throw new IllegalStateException("Expect %s state, but was: %s".formatted(
                                s,
                                state.get()
                        ));
                    }
                    final var session = event.session();
                    final var newSession = QwenTtsRealtimeSession.newBuilder(session)
                            .model(session.model())
                            .build();
                    final var qwenTtsRealtimeEmitter = new QwenTtsRealtimeEmitterImpl(newSession, emitter, futureMap);
                    delegate.onOpen(qwenTtsRealtimeEmitter);
                } else {
                    throw new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "session.created",
                            output.type()
                    ));
                }
            }
            case HANDSHAKE_COMPLETED -> delegate.onData(output);
        }
        ;
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        throw new UnsupportedOperationException("Binary data is not supported");
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
        HANDSHAKE_COMPLETED
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
        public void close() {
            CompletableFuture.completedStage(null)
                    .thenCompose(unused -> {
                        final var finishF = new CompletableFuture<Void>();
                        futureMap.put(KEY_SESSION_FINISHED, finishF);
                        data(new SessionFinishClientEvent(genUUID22()));
                        return finishF;
                    })
                    .whenComplete((unused, ex) -> {
                        futureMap.remove(KEY_SESSION_FINISHED);
                        super.close();
                    });
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return session;
        }

    }

}
