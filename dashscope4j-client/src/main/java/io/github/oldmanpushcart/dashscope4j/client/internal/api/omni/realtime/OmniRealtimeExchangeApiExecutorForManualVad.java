package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseDoneServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureSlot;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class OmniRealtimeExchangeApiExecutorForManualVad {

    private static final String KEY_BUFFER_CLEARED = "input_audio_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";
    private static final String KEY_RESPONSE_CREATED = "response.created";
    private static final String KEY_RESPONSE_DONE = "response.done";

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForManualVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    private static Parameters adjust(Parameters parameters) {
        final var newParameters = new Parameters().merge(parameters);
        final var newTurnDetection = Optional.ofNullable(parameters.get(OmniRealtimeParameterKeys.TURN_DETECTION))
                .map(turnDetection -> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD,
                        turnDetection.threshold(),
                        turnDetection.silence()
                ))
                .orElseGet(() -> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD,
                        null,
                        null
                ));
        newParameters.append(OmniRealtimeParameterKeys.TURN_DETECTION, newTurnDetection);
        return newParameters;
    }

    public CompletionStage<OmniRealtimeExchange.ManualVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var futureSlot = new FutureSlot<String>();
        return exchangeApi
                .newExchange(adjust(parameters), model, new OmniRealtimeExchange.Handler() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                        handler.onOpen(exchange);
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        /*
                         * 根据应答响应的状态来对返回等待结果进行处理
                         * 1. 如果响应状态为CANCELLED，则取消等待结果
                         * 2. 如果响应状态为FAILED，则取消等待结果并抛出异常
                         */
                        if (data instanceof OmniRealtimeResponseDoneServerEvent responseDoneEvent) {
                            final var response = responseDoneEvent.response();
                            switch (response.status()) {
                                case CANCELLED -> futureSlot.cancel(KEY_RESPONSE_DONE, true);
                                case FAILED -> {
                                    final var cause = new IllegalStateException("Response failed!");
                                    futureSlot.completeExceptionally(KEY_RESPONSE_DONE, cause);
                                }
                            }
                        }

                        /*
                         * 检查会话响应类型是否为ManualVad
                         * 因为整个Manual类型的会话是需要严格保障Event传递的顺序的，所以类型错乱会引起不确定后果
                         */
                        if (data instanceof OmniRealtimeSessionUpdatedServerEvent sessionUpdatedEvent) {
                            final var session = sessionUpdatedEvent.session();
                            final var turnDetection = session.parameters().get(OmniRealtimeParameterKeys.TURN_DETECTION);
                            if (turnDetection.type() != OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD) {
                                throw new IllegalStateException("Invalid turn detection type: %s".formatted(turnDetection.type()));
                            }
                        }

                        futureSlot.complete(data.type());
                        return handler.onData(data);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        futureSlot.drain().forEach((k, f) -> f.completeExceptionally(ex));
                        handler.onClosed(ex);
                    }

                })
                .thenApply(exchange -> new ManualVad(exchange, futureSlot));
    }


    private static class ManualVad extends Exchange.Proxy<OmniRealtimeClientEvent> implements OmniRealtimeExchange.ManualVad {

        private final Exchange<OmniRealtimeClientEvent> origin;
        private final FutureSlot<String> futureSlot;
        private final AtomicReference<State> stateRef = new AtomicReference<>();

        private ManualVad(Exchange<OmniRealtimeClientEvent> origin, FutureSlot<String> futureSlot) {
            super(origin);
            this.origin = origin;
            this.futureSlot = futureSlot;
        }

        @Override
        public CompletionStage<InputOp> newInput() {
            if (!stateRef.compareAndSet(State.IDLE, State.INPUT)) {
                throw new IllegalStateException();
            }
            return CompletableFuture.completedStage(new InputOpImpl());
        }

        private class InputOpImpl implements InputOp {

            @Override
            public CompletionStage<InputOp> image(BufferedImage image) {
                if (stateRef.get() != State.INPUT) {
                    throw new IllegalStateException();
                }
                final var event = new OmniRealtimeBufferAppendImageClientEvent(genUUID22(), image);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> audio(ByteBuffer buffer) {
                if (stateRef.get() != State.INPUT) {
                    throw new IllegalStateException();
                }
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> audio(byte[] bytes, int offset, int length) {
                if (stateRef.get() != State.INPUT) {
                    throw new IllegalStateException();
                }
                final var buffer = ByteBuffer.wrap(bytes, offset, length);
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<InputOp> clear() {
                if (!stateRef.compareAndSet(State.INPUT, State.IDLE)) {
                    throw new IllegalStateException();
                }
                final var clearedF = futureSlot.acquire(KEY_BUFFER_CLEARED);
                final var event = new OmniRealtimeBufferClearClientEvent(genUUID22());
                return origin.send(event)
                        .thenCompose(unused -> clearedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_CLEARED, clearedF))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {
                if (!stateRef.compareAndSet(State.INPUT, State.COMMITTED)) {
                    throw new IllegalStateException();
                }
                final var committedF = futureSlot.acquire(KEY_BUFFER_COMMITTED);
                final var event = new OmniRealtimeBufferCommitClientEvent(genUUID22());
                return origin.send(event)
                        .thenCompose(unused -> committedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_COMMITTED, committedF))
                        .thenApply(unused -> new ResponseOpImpl());
            }

            @Override
            public CompletionStage<Void> cancel() {
                return clear()
                        .thenAccept(v -> {
                        });
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            @Override
            public CompletableFuture<Void> create() {

                final var createdF = futureSlot.acquire(KEY_RESPONSE_CREATED);
                final var doneF = futureSlot.acquire(KEY_RESPONSE_DONE);
                final var createE = new OmniRealtimeResponseCreateClientEvent(genUUID22());
                return origin.send(createE)
                        .thenCompose(unused -> createdF)
                        .thenCompose(unused -> doneF)
                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            if (cause instanceof CancellationException && !doneF.isDone()) {
                                final var cancelE = new OmniRealtimeResponseCancelClientEvent(genUUID22());
                                return origin.send(cancelE)
                                        .thenCompose(unused -> doneF)
                                        .thenCompose(unused -> CompletableFuture.failedStage(ex));
                            } else {
                                return CompletableFuture.failedStage(ex);
                            }
                        })
                        .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_DONE, doneF))
                        .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_CREATED, createdF))
                        .toCompletableFuture();
            }

        }


        /**
         * 会话状态机（支持多轮手动 VAD 交互）。
         *
         * <p><strong>状态流转：</strong>
         * IDLE → (newInput) → INPUT → (commit) → COMMITTED → (create) → RESPONSE → (完成/取消) → IDLE
         *
         * <p><strong>关键约定：</strong>
         * <ul>
         *   <li>仅在 {@code IDLE} 状态允许调用 {@link OmniRealtimeExchangeApiExecutorForManualVad.ManualVad#newInput()}；</li>
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
