package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
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
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ManualVadOmniRealtimeConnectHandler extends OmniRealtimeConnectHandler<OmniRealtimeExchange.ManualVad> {

    private final FutureGuard<Void> bufferOpClearGuard = new FutureGuard<>();
    private final FutureGuard<Void> bufferOpCommitGuard = new FutureGuard<>();
    private final FutureGuard<Void> responseOpCreateGuard = new FutureGuard<>();
    private final FutureGuard<Void> responseOpCancelGuard = new FutureGuard<>();

    public ManualVadOmniRealtimeConnectHandler(Parameters parameters, OmniRealtimeExchange.ManualVad.Handler handler) {
        super(adjustParameters(parameters), handler);
    }

    private static Parameters adjustParameters(Parameters parameters) {

        final var turnDetection = Optional.ofNullable(parameters.get(OmniRealtimeParameterKeys.TURN_DETECTION))
                .map(v-> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD,
                        v.threshold(),
                        v.silence()
                ))
                .orElse(new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.MANUAL_VAD,
                        null,
                        null
                ));
        parameters.append(OmniRealtimeParameterKeys.TURN_DETECTION, turnDetection);

        return parameters;
    }

    @Override
    protected CompletionStage<OmniRealtimeExchange.ManualVad> processOnConnect(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.completedStage(new ManualVadImpl(exchange));
    }

    @Override
    protected CompletionStage<Void> processOnData(OmniRealtimeServerEvent event) {
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
        return CompletableFuture.completedStage(null);
    }

    @Override
    protected CompletionStage<Void> processOnBinary(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    protected CompletionStage<Void> processOnClose(Throwable ex) {
        return CompletableFuture.completedStage(null);
    }

    private class ManualVadImpl implements OmniRealtimeExchange.ManualVad {

        private final Exchange<OmniRealtimeClientEvent> origin;

        private ManualVadImpl(Exchange<OmniRealtimeClientEvent> origin) {
            this.origin = origin;
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

        private class BufferOpImpl implements ManualVad.BufferOp {

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
            public CompletionStage<ManualVad.BufferOp> clear() {
                final var future = bufferOpClearGuard.acquire();
                return CompletableFuture.<Void>completedStage(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeBufferClearClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> future)
                        .whenComplete((v, ex) -> bufferOpClearGuard.release(future))
                        .thenApply(v -> this)
                        ;
            }

            @Override
            public CompletionStage<ManualVad.ResponseOp> commit() {
                final var future = bufferOpCommitGuard.acquire();
                return CompletableFuture.<Void>completedStage(null)
                        .thenCompose(unused -> origin.send(new OmniRealtimeBufferCommitClientEvent(StringUtils.uuid())))
                        .thenCompose(unused -> future)
                        .whenComplete((v, ex) -> bufferOpCommitGuard.release(future))
                        .thenApply(v -> new ResponseOpImpl())
                        ;
            }

        }

        private class ResponseOpImpl implements ManualVad.ResponseOp {

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
