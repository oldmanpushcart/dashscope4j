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
    public void onData(ServerEvent output) {
        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }
        delegate.onData(output);
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        delegate.onBinary(buffer);
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

        private volatile boolean inputted = false;

        private ManualVadImpl(Map<String, CompletableFuture<?>> futureMap, QwenTtsRealtimeEmitter delegate) {
            super(delegate);
            this.futureMap = futureMap;
            this.delegate = delegate;
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return delegate.session();
        }

        @Override
        public InputOp newInput() {

            if (!state.compareAndSet(State.IDLE, State.INPUT)) {
                throw new IllegalStateException("Expect state %s, but was %s".formatted(
                        State.IDLE,
                        state.get()
                ));
            }

            return new InputOpImpl();
        }


        private class InputOpImpl implements InputOp {

            private volatile boolean committed = false;

            @Override
            public InputOp text(String text) {

                if (committed) {
                    throw new IllegalStateException("Already committed!");
                }

                if (state.get() != State.INPUT) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                data(new BufferAppendTextClientEvent(genUUID22(), text));
                inputted = true;
                return this;
            }

            @Override
            public CompletionStage<InputOp> clear() {

                if (committed) {
                    throw new IllegalStateException("Already committed!");
                }

                if (!state.compareAndSet(State.INPUT, State.CLEAR)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                if (!inputted) {
                    state.compareAndSet(State.CLEAR, State.INPUT);
                    return CompletableFuture.completedStage(this);
                }

                return CompletableFuture.completedStage(null)
                        .thenCompose(unused -> {
                            final var clearF = new CompletableFuture<Void>();
                            futureMap.put(KEY_BUFFER_CLEARED, clearF);
                            data(new BufferClearClientEvent(genUUID22()));
                            return clearF;
                        })
                        .whenComplete(((unused, ex) -> {
                            futureMap.remove(KEY_BUFFER_CLEARED);
                            state.compareAndSet(State.CLEAR, State.INPUT);
                        }))
                        .thenApply(unused -> {
                            inputted = false;
                            return this;
                        });
            }

            @Override
            public CompletionStage<Void> commit() {

                if (committed) {
                    throw new IllegalStateException("Already committed!");
                }

                if (!state.compareAndSet(State.INPUT, State.COMMIT)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                return CompletableFuture.completedStage(null)
                        .thenCompose(unused -> {

                            final var commitF = new CompletableFuture<Void>();
                            futureMap.put(KEY_BUFFER_COMMITTED, commitF);

                            final var doneF = new CompletableFuture<Void>();
                            futureMap.put(KEY_RESPONSE_DONE, doneF);

                            data(new BufferCommitClientEvent(genUUID22()));
                            return commitF.thenCompose(u -> doneF);
                        })
                        .whenComplete(((unused, ex) -> {
                            futureMap.remove(KEY_BUFFER_COMMITTED);
                            futureMap.remove(KEY_RESPONSE_DONE);
                            state.compareAndSet(State.COMMIT, null != ex ? State.INPUT : State.IDLE);
                        }))
                        .thenAccept(unused -> {
                            committed = true;
                        });

            }

        }

        private enum State {

            IDLE,
            INPUT,
            CLEAR,
            COMMIT

        }

    }

}
