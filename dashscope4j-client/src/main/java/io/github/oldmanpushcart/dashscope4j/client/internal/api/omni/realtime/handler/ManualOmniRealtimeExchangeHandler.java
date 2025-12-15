package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeBufferClearedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeBufferCommittedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeResponseDoneServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureGuard;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ManualOmniRealtimeExchangeHandler extends OmniRealtimeExchangeHandler<OmniRealtimeExchange.Manual> {

    private final OmniRealtimeExchange.Manual.Handler handler;

    private final FutureGuard<Void> bufferOpClearGuard = new FutureGuard<>();
    private final FutureGuard<Void> bufferOpCommitGuard = new FutureGuard<>();
    private final FutureGuard<Void> responseOpCreateGuard = new FutureGuard<>();
    private final FutureGuard<Void> responseOpCancelGuard = new FutureGuard<>();

    public ManualOmniRealtimeExchangeHandler(Parameters parameters, OmniRealtimeExchange.Manual.Handler handler) {
        super(parameters);
        this.handler = handler;
    }

    @Override
    protected CompletionStage<OmniRealtimeExchange.Manual> make(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.completedStage(new ExchangeImpl(exchange));
    }

    @Override
    public CompletionStage<OmniRealtimeExchange.Manual> onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.completedStage(exchange)
                .thenCompose(super::onOpen)
                .thenCompose(handler::onOpen);
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {

        if (event instanceof OmniRealtimeBufferClearedServerEvent) {
            bufferOpClearGuard.completed(null);
        } else if (event instanceof OmniRealtimeBufferCommittedServerEvent) {
            bufferOpCommitGuard.completed(null);
        } else if (event instanceof OmniRealtimeResponseDoneServerEvent responseDoneEvent) {

            switch (responseDoneEvent.response().status()) {
                case CANCELLED -> responseOpCancelGuard.completed(null);
                case COMPLETED -> responseOpCreateGuard.completed(null);
                case FAILED -> responseOpCreateGuard.completeExceptionally(new RuntimeException("Response failed"));
            }
        }

        return handler.onData(event);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return handler.onBinary(buffer);
    }

    @Override
    public CompletionStage<Void> onClosed(Throwable ex) {
        return handler.onClosed(ex);
    }

    private class ExchangeImpl implements OmniRealtimeExchange.Manual {

        private final Exchange<OmniRealtimeClientEvent> origin;

        private ExchangeImpl(Exchange<OmniRealtimeClientEvent> origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<BufferOp> newConversation() {
            return null;
        }

        @Override
        public String uuid() {
            return origin.uuid();
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

        private class BufferOpImpl implements OmniRealtimeExchange.Manual.BufferOp {

            @Override
            public CompletionStage<OmniRealtimeExchange.Manual.BufferOp> image(BufferedImage image) {
                final var event = new OmniRealtimeBufferAppendImageClientEvent(StringUtils.uuid(), image);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<OmniRealtimeExchange.Manual.BufferOp> audio(ByteBuffer buffer) {
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(StringUtils.uuid(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<OmniRealtimeExchange.Manual.BufferOp> audio(byte[] bytes, int offset, int length) {
                final var buffer = ByteBuffer.wrap(bytes, offset, length);
                final var event = new OmniRealtimeBufferAppendAudioClientEvent(StringUtils.uuid(), buffer);
                return origin.send(event)
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<OmniRealtimeExchange.Manual.BufferOp> clear() {
                final var future = bufferOpClearGuard.acquire();
                return CompletableFuture.<Void>completedStage(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeBufferClearClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> future)
                        .whenComplete((v, ex) -> bufferOpClearGuard.release(future))
                        .thenApply(v -> this)
                        ;
            }

            @Override
            public CompletionStage<OmniRealtimeExchange.Manual.ResponseOp> commit() {
                final var future = bufferOpCommitGuard.acquire();
                return CompletableFuture.<Void>completedStage(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeBufferCommitClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> future)
                        .whenComplete((v, ex) -> bufferOpCommitGuard.release(future))
                        .thenApply(v -> new ResponseOpImpl())
                        ;
            }

        }

        private class ResponseOpImpl implements OmniRealtimeExchange.Manual.ResponseOp {

            private CompletionStage<Void> cancel() {
                final var cancelF = responseOpCancelGuard.acquire();
                return CompletableFuture.<Void>completedStage(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeResponseCancelClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> cancelF)
                        .whenComplete((v, ex) -> responseOpCancelGuard.release(cancelF))
                        ;
            }

            @Override
            public CompletableFuture<Void> create() {
                final var createF = responseOpCreateGuard.acquire();
                return CompletableFuture.<Void>completedFuture(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeResponseCreateClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> createF)
                        .exceptionallyCompose(ex -> {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            return cause instanceof CancellationException
                                    ? cancel()
                                    : CompletableFuture.failedStage(null == cause ? ex : cause);
                        })
                        .whenComplete((v, ex) -> responseOpCreateGuard.release(createF))
                        ;
            }

        }

    }


}
