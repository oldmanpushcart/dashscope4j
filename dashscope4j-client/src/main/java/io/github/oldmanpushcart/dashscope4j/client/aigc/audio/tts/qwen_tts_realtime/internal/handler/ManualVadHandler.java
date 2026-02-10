package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.BufferAppendTextClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.BufferClearClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.BufferCommitClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ManualVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_BUFFER_CLEARED = "input_text_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_text_buffer.committed";
    private static final String KEY_RESPONSE_DONE = "response.done";

    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;
    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();

    public ManualVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var manualVad = new ManualVadImpl(futureMap, (QwenTtsRealtimeEmitter) emitter);
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
            implements QwenTtsRealtimeEmitter.ManualVad {

        private final Map<String, CompletableFuture<?>> futureMap;
        private final QwenTtsRealtimeEmitter delegate;
        private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

        private ManualVadImpl(Map<String, CompletableFuture<?>> futureMap, QwenTtsRealtimeEmitter delegate) {
            super(delegate);
            this.futureMap = futureMap;
            this.delegate = delegate;
        }

        private void changeState(State expect, State update) {
            if (!state.compareAndSet(expect, update)) {
                throw new IllegalStateException("Operation requires %s state, but current state is: %s".formatted(expect, state.get()));
            }
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
        public CompletionStage<InputOp> newInput() {
            changeState(State.IDLE, State.INPUT_READY);
            return CompletableFuture.completedStage(new InputOpImpl());
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return delegate.session();
        }

        private class InputOpImpl implements InputOp {

            @Override
            public CompletionStage<InputOp> text(String text) {

                state.compareAndSet(State.INPUT_READY, State.INPUT);
                if (state.get() != State.INPUT) {
                    throw new IllegalStateException("Operation requires %s state!".formatted(state.get()));
                }

                final var event = new BufferAppendTextClientEvent(genUUID22(), text);
                return data(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> clear() {
                final var s = state.get();
                if (s == State.INPUT_READY) {
                    return CompletableFuture.completedStage(this);
                }

                changeState(State.INPUT, State.INPUT_READY);
                final var clearF = new CompletableFuture<Void>();
                register(KEY_BUFFER_CLEARED, clearF);
                final var event = new BufferClearClientEvent(genUUID22());
                return data(event)
                        .thenCompose(unused -> clearF)
                        .whenComplete((unused, ex) -> unregister(KEY_BUFFER_CLEARED, ex))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ManualVad> commit() {

                final var s = state.get();
                if (s == State.INPUT_READY) {
                    return CompletableFuture.completedStage(ManualVadImpl.this);
                }

                changeState(State.INPUT, State.COMMIT);
                final var commitF = new CompletableFuture<Void>();
                final var doneF = new CompletableFuture<Void>();
                register(KEY_BUFFER_COMMITTED, commitF);
                register(KEY_RESPONSE_DONE, doneF);
                final var event = new BufferCommitClientEvent(genUUID22());
                return data(event)
                        .thenCompose(unused -> commitF)
                        .whenComplete((unused, ex) -> unregister(KEY_BUFFER_COMMITTED, ex))
                        .thenCompose(unused -> doneF)
                        .whenComplete((unused, ex) -> unregister(KEY_RESPONSE_DONE, ex))
                        .thenApply(unused -> {
                            state.set(State.IDLE);
                            return ManualVadImpl.this;
                        });
            }

        }

        private enum State {

            IDLE,
            INPUT_READY,
            INPUT,
            COMMIT

        }

    }

}
