package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureSlot;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

class ManualVadHandler implements OmniRealtimeExchange.Handler {

    private static final String KEY_BUFFER_CLEARED = "input_audio_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";
    private static final String KEY_RESPONSE_CREATED = "response.created";
    private static final String KEY_RESPONSE_DONE = "response.done";

    private final FutureSlot<String> futureSlot = new FutureSlot<>();
    private final OmniRealtimeExchange.Handler delegate;
    private final CompletableFuture<ManualVad> completeF = new CompletableFuture<>();

    public ManualVadHandler(OmniRealtimeExchange.Handler delegate) {
        this.delegate = delegate;
    }

    public CompletionStage<ManualVad> completeStage() {
        return completeF;
    }

    @Override
    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
        final var manualVad = new ManualVadImpl(exchange, futureSlot);
        delegate.onOpen(manualVad);
        completeF.complete(manualVad);
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {
        futureSlot.complete(data.type());
        return delegate.onData(data);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public void onClosed(Throwable ex) {
        futureSlot.drain().forEach((k, f) -> f.completeExceptionally(ex));
        delegate.onClosed(ex);
        completeF.completeExceptionally(ex);
    }


    private static class ManualVadImpl extends Exchange.Proxy<OmniRealtimeClientEvent> implements ManualVad {

        private final Exchange<OmniRealtimeClientEvent> origin;
        private final FutureSlot<String> futureSlot;
        private final AtomicReference<State> stateRef = new AtomicReference<>(State.IDLE);

        private ManualVadImpl(Exchange<OmniRealtimeClientEvent> origin, FutureSlot<String> futureSlot) {
            super(origin);
            this.origin = origin;
            this.futureSlot = futureSlot;
        }

        private boolean tryChangeState(State expect, State update) {
            return stateRef.compareAndSet(expect, update);
        }

        private void changeState(State expect, State update) {
            if (!tryChangeState(expect, update)) {
                throw new IllegalStateException("Operation requires %s state, but current state is: %s".formatted(expect, stateRef.get()));
            }
        }

        private void checkState(State expect) {
            if (expect != stateRef.get()) {
                throw new IllegalStateException("Operation requires %s state, but current state is: %s".formatted(expect, stateRef.get()));
            }
        }

        @Override
        public CompletionStage<InputOp> newInput() {
            changeState(State.IDLE, State.INPUT_READY);
            return CompletableFuture.completedStage(new InputOpImpl());
        }

        private class InputOpImpl implements InputOp {

            @Override
            public CompletionStage<InputOp> image(BufferedImage image) {
                tryChangeState(State.INPUT_READY, State.INPUT);
                checkState(State.INPUT);
                final var event = new OmniRealtimeBufferAppendImageClientEvent(genUUID22(), image);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> audio(ByteBuffer buffer) {
                tryChangeState(State.INPUT_READY, State.INPUT);
                checkState(State.INPUT);
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> audio(byte[] bytes, int offset, int length) {
                tryChangeState(State.INPUT_READY, State.INPUT);
                checkState(State.INPUT);
                final var buffer = ByteBuffer.wrap(bytes, offset, length);
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.send(event)
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
                final var clearedF = futureSlot.acquire(KEY_BUFFER_CLEARED);
                final var event = new OmniRealtimeBufferClearClientEvent(genUUID22());
                return origin.send(event)
                        .thenCompose(unused -> clearedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_CLEARED, clearedF))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {
                changeState(State.INPUT, State.COMMITTED);
                final var committedF = futureSlot.acquire(KEY_BUFFER_COMMITTED);
                final var event = new OmniRealtimeBufferCommitClientEvent(genUUID22());
                return origin.send(event)
                        .thenCompose(unused -> committedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_COMMITTED, committedF))
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
                changeState(State.COMMITTED, State.RESPONSE);
                final var createdF = futureSlot.acquire(KEY_RESPONSE_CREATED);
                final var doneF = futureSlot.acquire(KEY_RESPONSE_DONE);
                final var createE = new OmniRealtimeResponseCreateClientEvent(genUUID22());
                return origin.send(createE)

                        .thenCompose(unused -> createdF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_CREATED, createdF))

                        .thenCompose(unused -> doneF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_DONE, doneF))

                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            if(!(cause instanceof CancellationException)) {
                                return CompletableFuture.failedStage(cause);
                            }
                            final var cancelE = new OmniRealtimeResponseCancelClientEvent(genUUID22());
                            return origin.send(cancelE)
                                    .thenCompose(unused -> doneF)
                                    .whenComplete((v, unusedEx) -> futureSlot.release(KEY_RESPONSE_DONE, doneF));
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
         *   <li>仅在 {@code IDLE} 状态允许调用 {@link OmniRealtimeExchange.ManualVad#newInput()}；</li>
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
             * 输入已就绪，等待开始输入。
             * <p>在此状态下，调用 {@code audio()}/{@code image()} 将切换至 {@code INPUT}。
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
            COMMITTED,

            /**
             * 服务端正在生成并流式返回响应。
             * <p>响应完成后（无论成功、失败或取消），会话必须自动回到 {@code IDLE}。
             */
            RESPONSE

        }

    }

}
