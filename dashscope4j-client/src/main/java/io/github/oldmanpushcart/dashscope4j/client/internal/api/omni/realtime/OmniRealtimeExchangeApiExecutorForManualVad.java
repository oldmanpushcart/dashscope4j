package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.*;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class OmniRealtimeExchangeApiExecutorForManualVad {

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForManualVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    public CompletionStage<OmniRealtimeExchange.ManualVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var futureMap = new ConcurrentHashMap<Class<?>, CompletableFuture<?>>();
        return exchangeApi
                .newExchange(parameters, model, new OmniRealtimeExchange.Handler() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                        handler.onOpen(exchange);
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        if (data instanceof OmniRealtimeBufferClearedServerEvent) {
                            Optional.ofNullable(futureMap.remove(OmniRealtimeBufferClearedServerEvent.class))
                                    .orElseThrow(() -> new IllegalStateException(""))
                                    .complete(null);
                        }

                        if (data instanceof OmniRealtimeBufferCommittedServerEvent) {
                            Optional.ofNullable(futureMap.remove(OmniRealtimeBufferCommittedServerEvent.class))
                                    .orElseThrow(() -> new IllegalStateException(""))
                                    .complete(null);
                        }

                        if (data instanceof OmniRealtimeResponseCreatedServerEvent) {
                            Optional.ofNullable(futureMap.remove(OmniRealtimeResponseCreatedServerEvent.class))
                                    .orElseThrow(() -> new IllegalStateException(""))
                                    .complete(null);
                        }


                        return handler.onData(data);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        futureMap.forEach((clazz, future) -> future.completeExceptionally(ex));
                        futureMap.clear();
                        handler.onClosed(ex);
                    }

                })
                .thenApply(exchange -> new ManualVad(exchange, futureMap));
    }


    private static class ManualVad implements OmniRealtimeExchange.ManualVad {

        private final Exchange<OmniRealtimeClientEvent> origin;
        private final ConcurrentHashMap<Class<?>, CompletableFuture<?>> futureMap;

        private ManualVad(Exchange<OmniRealtimeClientEvent> origin, ConcurrentHashMap<Class<?>, CompletableFuture<?>> futureMap) {
            this.origin = origin;
            this.futureMap = futureMap;
        }

        @Override
        public CompletionStage<BufferOp> newConversation() {
            return CompletableFuture.completedStage(new BufferOpImpl());
        }

        @Override
        public String id() {
            return origin.id();
        }

        @Override
        public boolean isClosed() {
            return origin.isClosed();
        }

        @Override
        public CompletionStage<Void> closing() {
            return origin.closing();
        }

        @Override
        public void close() {
            origin.close();
        }

        @Override
        public CompletionStage<Void> send(OmniRealtimeClientEvent data) {
            return origin.send(data);
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            return origin.send(buffer);
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

                final var future = new CompletableFuture<>();
                if (null != futureMap.putIfAbsent(OmniRealtimeBufferClearedServerEvent.class, future)) {
                    throw new IllegalStateException();
                }

                final var event = new OmniRealtimeBufferClearClientEvent(StringUtils.uuid());
                return origin.send(event)
                        .thenCompose(unused -> future)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {

                final var future = new CompletableFuture<>();
                if (null != futureMap.putIfAbsent(OmniRealtimeBufferCommittedServerEvent.class, future)) {
                    throw new IllegalStateException();
                }

                final var event = new OmniRealtimeBufferCommitClientEvent(StringUtils.uuid());
                return origin.send(event)
                        .thenCompose(unused -> future)
                        .thenApply(unused -> new ResponseOpImpl());
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            @Override
            public CompletableFuture<Void> create() {

                final var createdF = new CompletableFuture<Void>();
                if (null != futureMap.putIfAbsent(OmniRealtimeResponseCreatedServerEvent.class, createdF)) {
                    throw new IllegalStateException();
                }

                final var doneF = new CompletableFuture<Void>();
                if (null != futureMap.putIfAbsent(OmniRealtimeResponseDoneServerEvent.class, doneF)) {
                    throw new IllegalStateException();
                }

                final var createE = new OmniRealtimeResponseCreateClientEvent(StringUtils.uuid());
                return origin.send(createE)
                        .thenCompose(unused -> createdF)
                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            if (cause instanceof CancellationException && !doneF.isDone()) {
                                final var cancelE = new OmniRealtimeResponseCancelClientEvent(StringUtils.uuid());
                                return origin.send(cancelE);
                            } else {
                                return CompletableFuture.failedStage(ex);
                            }
                        })
                        .thenCompose(unused -> doneF)
                        .toCompletableFuture();
            }

        }

    }

}
