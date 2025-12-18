package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseDoneServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureSlot;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeExchangeApiExecutorForManualVad {

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForManualVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    private static final String KEY_BUFFER_CLEARED = "input_audio_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";
    private static final String KEY_RESPONSE_CREATED = "response.created";
    private static final String KEY_RESPONSE_DONE = "response.done";


    public CompletionStage<OmniRealtimeExchange.ManualVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var futureSlot = new FutureSlot<String>();
        return exchangeApi
                .newExchange(parameters, model, new OmniRealtimeExchange.Handler() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                        handler.onOpen(exchange);
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        if(data instanceof OmniRealtimeResponseDoneServerEvent responseDoneEvent) {
                            final var doneF = futureSlot.get(KEY_RESPONSE_DONE);
                            if(null != doneF
                                    && !doneF.isDone()
                                    && responseDoneEvent.response().status() == OmniRealtimeServerEvent.Status.CANCELLED) {
                                doneF.cancel(true);
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

        private ManualVad(Exchange<OmniRealtimeClientEvent> origin, FutureSlot<String> futureSlot) {
            super(origin);
            this.origin = origin;
            this.futureSlot = futureSlot;
        }

        @Override
        public CompletionStage<BufferOp> newBuffer() {
            return CompletableFuture.completedStage(new BufferOpImpl());
        }

        private class BufferOpImpl implements BufferOp {

            @Override
            public CompletionStage<ManualVad.BufferOp> image(BufferedImage image) {
                final var event = new OmniRealtimeBufferAppendImageClientEvent(StringUtils.uuid(), image);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ManualVad.BufferOp> audio(ByteBuffer buffer) {
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(StringUtils.uuid(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ManualVad.BufferOp> audio(byte[] bytes, int offset, int length) {
                final var buffer = ByteBuffer.wrap(bytes, offset, length);
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(StringUtils.uuid(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<BufferOp> clear() {
                final var clearedF = futureSlot.acquire(KEY_BUFFER_CLEARED);
                final var event = new OmniRealtimeBufferClearClientEvent(StringUtils.uuid());
                return origin.send(event)
                        .thenCompose(unused -> clearedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_CLEARED, clearedF))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {
                final var committedF = futureSlot.acquire(KEY_BUFFER_COMMITTED);
                final var event = new OmniRealtimeBufferCommitClientEvent(StringUtils.uuid());
                return origin.send(event)
                        .thenCompose(unused -> committedF)
                        .whenComplete((v, ex) -> futureSlot.release(KEY_BUFFER_COMMITTED, committedF))
                        .thenApply(unused -> new ResponseOpImpl());
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            @Override
            public CompletableFuture<Void> create() {

                try {
                    Thread.sleep(1000*1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // final var createdF = futureSlot.acquire(KEY_RESPONSE_CREATED);
                final var doneF = futureSlot.acquire(KEY_RESPONSE_DONE);
                final var createE = new OmniRealtimeResponseCreateClientEvent(StringUtils.uuid());
                return origin.send(createE)
                        // .thenCompose(unused -> createdF)
                        .thenCompose(unused -> doneF)
                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            if (cause instanceof CancellationException && !doneF.isDone()) {
                                final var cancelE = new OmniRealtimeResponseCancelClientEvent(StringUtils.uuid());
                                return origin.send(cancelE)
                                        .thenCompose(unused -> doneF)
                                        .thenCompose(unused -> CompletableFuture.failedStage(ex));
                            } else {
                                return CompletableFuture.failedStage(ex);
                            }
                        })
                        .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_DONE, doneF))
                        // .whenComplete((v, ex) -> futureSlot.release(KEY_RESPONSE_CREATED, createdF))
                        .toCompletableFuture();
            }

        }

    }

}
