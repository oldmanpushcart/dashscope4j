package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.BufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.BufferCommitClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ManualVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";

    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;
    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();

    public ManualVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var manualVad = new ManualVadImpl(futureMap, (QwenAsrRealtimeEmitter) emitter);
        delegate.onOpen(manualVad);
    }

    @Override
    public CompletionStage<Void> onData(ServerEvent output) {
        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }
        return delegate.onData(output);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return delegate.onBinary(buffer);
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


    private static class ManualVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenAsrRealtimeEmitter.ManualVad {

        private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
        private final Map<String, CompletableFuture<?>> futureMap;
        private final QwenAsrRealtimeEmitter delegate;

        public ManualVadImpl(Map<String, CompletableFuture<?>> futureMap, QwenAsrRealtimeEmitter delegate) {
            super(delegate);
            this.futureMap = futureMap;
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<InputOp> newInput() {
            if (!state.compareAndSet(State.IDLE, State.INPUT)) {
                throw new IllegalStateException("Expect state %s, but found %s".formatted(
                        State.IDLE,
                        state.get()
                ));
            }
            return CompletableFuture.completedStage(new InputOpImpl());
        }

        @Override
        public QwenAsrRealtimeSession session() {
            return delegate.session();
        }

        private class InputOpImpl implements InputOp {

            @Override
            public CompletionStage<InputOp> audio(ByteBuffer buffer) {
                final var event = new BufferAppendAudioClientEvent(genUUID22(), buffer);
                return data(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ManualVad> commit() {
                if (!state.compareAndSet(State.INPUT, State.COMMIT)) {
                    throw new IllegalStateException("Expect state %s, but found %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }
                final var commitF = new CompletableFuture<>();
                if (futureMap.putIfAbsent(KEY_BUFFER_COMMITTED, commitF) != null) {
                    throw new IllegalStateException("Exists running commit!");
                }
                final var event = new BufferCommitClientEvent(genUUID22());
                return data(event)
                        .thenCompose(unused -> commitF)
                        .whenComplete((unused, ex) -> futureMap.remove(KEY_BUFFER_COMMITTED))
                        .thenApply(unused -> {
                            state.set(State.IDLE);
                            return ManualVadImpl.this;
                        });
            }

        }

        private enum State {
            IDLE,
            INPUT,
            COMMIT
        }

    }

}
