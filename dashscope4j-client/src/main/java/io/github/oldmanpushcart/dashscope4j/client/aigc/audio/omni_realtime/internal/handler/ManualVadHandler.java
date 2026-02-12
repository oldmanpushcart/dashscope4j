package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ManualVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_BUFFER_CLEARED = "input_audio_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";
    private static final String KEY_RESPONSE_CREATED = "response.created";
    private static final String KEY_RESPONSE_DONE = "response.done";

    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    public ManualVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var manualVad = new ManualVadImpl((OmniRealtimeEmitter) emitter, futureMap);
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
        return CompletableFuture.completedStage(null);
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


    private static class ManualVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements ManualVad {

        private final OmniRealtimeEmitter origin;
        private final Map<String, CompletableFuture<?>> futureMap;
        private final AtomicReference<State> stateRef = new AtomicReference<>(State.IDLE);

        private ManualVadImpl(OmniRealtimeEmitter origin, Map<String, CompletableFuture<?>> futureMap) {
            super(origin);
            this.origin = origin;
            this.futureMap = futureMap;
        }

        private boolean tryChangeState(State expect, State update) {
            return stateRef.compareAndSet(expect, update);
        }

        private void changeState(State expect, State update) {
            if (!tryChangeState(expect, update)) {
                throw new IllegalStateException("Operation requires %s state, but current state is: %s".formatted(expect, stateRef.get()));
            }
        }

        private void requireInputState() {
            final var s = stateRef.get();
            if (s != State.INPUT) {
                throw new IllegalStateException("Operation requires %s state, but current state is: %s".formatted(State.INPUT, s));
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
        public OmniRealtimeSession session() {
            return origin.session();
        }

        private class InputOpImpl implements InputOp {

            @Override
            public CompletionStage<InputOp> image(ByteBuffer image) {
                tryChangeState(State.INPUT_READY, State.INPUT);
                requireInputState();
                final var event = new BufferAppendImageClientEvent(genUUID22(), image);
                return origin.data(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> audio(ByteBuffer buffer) {
                tryChangeState(State.INPUT_READY, State.INPUT);
                requireInputState();
                final var event = new BufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.data(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> clear() {

                /*
                 * 在没有任何输入之前，clear 操作无效
                 */
                if (stateRef.get() == State.INPUT_READY) {
                    return CompletableFuture.completedStage(this);
                }

                changeState(State.INPUT, State.INPUT_READY);
                final var clearF = new CompletableFuture<Void>();
                register(KEY_BUFFER_CLEARED, clearF);
                final var event = new BufferClearClientEvent(genUUID22());
                return origin.data(event)
                        .thenCompose(unused -> clearF)
                        .whenComplete((v, ex) -> unregister(KEY_BUFFER_CLEARED, ex))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {
                changeState(State.INPUT, State.COMMIT);
                final var commitF = new CompletableFuture<Void>();
                register(KEY_BUFFER_COMMITTED, commitF);
                final var event = new BufferCommitClientEvent(genUUID22());
                return origin.data(event)
                        .thenCompose(unused -> commitF)
                        .whenComplete((v, ex) -> unregister(KEY_BUFFER_COMMITTED, ex))
                        .thenApply(unused -> new ResponseOpImpl());
            }

            @Override
            public CompletionStage<Void> cancel() {

                /*
                 * 在没有任何输入之前，cancel 操作无效
                 */
                if (stateRef.get() == State.INPUT_READY) {
                    return CompletableFuture.completedStage(null);
                }

                changeState(State.INPUT, State.IDLE);
                return clear()
                        .thenAccept(v -> {
                        });
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            @Override
            public CompletableFuture<Void> create() {
                changeState(State.COMMIT, State.RESPONSE);
                final var createF = new CompletableFuture<Void>();
                final var doneF = new CompletableFuture<Void>();
                register(KEY_RESPONSE_CREATED, createF);
                register(KEY_RESPONSE_DONE, doneF);
                final var createE = new ResponseCreateClientEvent(genUUID22());
                return origin.data(createE)

                        .thenCompose(unused -> createF)
                        .whenComplete((v, ex) -> unregister(KEY_RESPONSE_CREATED, ex))

                        .thenCompose(unused -> doneF)
                        .whenComplete((v, ex) -> unregister(KEY_RESPONSE_DONE, ex))

                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            if (!(cause instanceof CancellationException)) {
                                return CompletableFuture.failedStage(cause);
                            }
                            final var cancelE = new ResponseCancelClientEvent(genUUID22());
                            return origin.data(cancelE)
                                    .thenCompose(unused -> doneF)
                                    .whenComplete((v, unusedEx) -> unregister(KEY_RESPONSE_DONE, unusedEx));
                        })

                        .whenComplete((v, ex) -> changeState(State.RESPONSE, State.IDLE))
                        .toCompletableFuture();
            }

        }


        /**
         * 会话状态机（支持多轮手动 VAD 交互）。
         *
         * <p><strong>状态流转：</strong>
         * IDLE → (newInput) → INPUT_READY → (audio/image) → INPUT → (commit) → COMMITTED → (create) → RESPONSE → (完成/取消) → IDLE
         *
         * <p><strong>关键约定：</strong>
         * <ul>
         *   <li>仅在 {@code IDLE} 状态允许调用 {@link OmniRealtimeEmitter.ManualVad#newInput()}；</li>
         *   <li>进入 {@code RESPONSE} 后，必须等待其完成（成功/失败/取消），然后自动回到 {@code IDLE}；</li>
         *   <li>从 {@code RESPONSE} 返回 {@code IDLE} 时，必须清理所有临时资源（buffer、图像、future 等）。</li>
         * </ul>
         */
        private enum State {

            /**
             * 会话空闲，可开始新一轮输入。
             * <p>在此状态下，调用 {@code newInput()} 将切换至 {@code INPUT}。
             */
            IDLE,

            /**
             * 提交准备已就绪，等待开始提交内容。
             * <p>在此状态下，调用 {@code audio()}/{@code image()} 将切换至 {@code SUBMISSION}。
             */
            INPUT_READY,

            /**
             * 正在接收多模态输入（音频和/或图像）。
             * <p>可通过 {@code audio()}/{@code image()} 追加数据，或通过 {@code clear()} 重置，
             * 或通过 {@code commit()}/{@code cancel()} 结束本轮输入。
             */
            INPUT,

            /**
             * 输入已提交，等待启动响应生成。
             * <p>此状态短暂存在，通常立即调用 {@code ResponseOp#create()} 进入 {@code RESPONSE}。
             */
            COMMIT,

            /**
             * 服务端正在生成并流式返回响应。
             * <p>响应完成后（无论成功、失败或取消），会话必须自动回到 {@code IDLE}。
             */
            RESPONSE

        }

    }

}
