package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.*;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class OmniRealtimeExchangeApiExecutorForManualVad {

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForManualVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    public CompletionStage<OmniRealtimeExchange.ManualVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var opGuard = new AtomicReference<OpFuture>();
        exchangeApi.newExchange(parameters, model, new OmniRealtimeExchange.Handler() {

            @Override
            public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                handler.onOpen(exchange);
            }

            @Override
            public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                final var opF = opGuard.get();
                if (OpFuture.matches(data, opF)) {
                    opF.complete(null);
                }

                return handler.onData(data);
            }

            @Override
            public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                return handler.onBinary(buffer);
            }

            @Override
            public void onClosed(Throwable ex) {

                final var opF = opGuard.getAndSet(null);
                if (null != opF) {
                    opF.completeExceptionally(ex);
                }

                handler.onClosed(ex);
            }

        });
    }


    private static class OpFuture extends CompletableFuture<Void> {

        private final State state;

        private OpFuture(State state) {
            this.state = state;
        }

        public State state() {
            return state;
        }

        public static boolean matches(OmniRealtimeServerEvent event, OpFuture opF) {
            return null != opF && opF.state().type().isInstance(event);
        }

        public enum State {

            WAITING_BUFFER_CLEARED(OmniRealtimeBufferClearedServerEvent.class),
            WAITING_BUFFER_COMMITED(OmniRealtimeBufferCommittedServerEvent.class),
            WAITING_RESPONSE_CREATED(OmniRealtimeResponseCreatedServerEvent.class),
            WAITING_RESPONSE_DONE(OmniRealtimeResponseDoneServerEvent.class),
            ;

            private final Class<?> type;

            State(Class<?> type) {
                this.type = type;
            }

            public Class<?> type() {
                return type;
            }

        }

    }

    private static class ManualVad implements OmniRealtimeExchange.ManualVad {

        private final Exchange<OmniRealtimeClientEvent> origin;
        private final AtomicReference<OpFuture> opGuard;

        private ManualVad(Exchange<OmniRealtimeClientEvent> origin, AtomicReference<OpFuture> opGuard) {
            this.origin = origin;
            this.opGuard = opGuard;
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
                final var event = new OmniRealtimeBufferClearClientEvent(StringUtils.uuid());
                final var opF = new OpFuture(OpFuture.State.WAITING_BUFFER_CLEARED);
                if (!opGuard.compareAndSet(null, opF)) {
                    throw new IllegalStateException();
                }
                return origin.send(event)
                        .thenCompose(unused -> opF)
                        .whenComplete((v, ex) -> opGuard.compareAndSet(opF, null))
                        .thenApply(unused -> this);
            }

            @Override
            public CompletionStage<ResponseOp> commit() {
                final var event = new OmniRealtimeBufferCommitClientEvent(StringUtils.uuid());
                final var opF = new OpFuture(OpFuture.State.WAITING_BUFFER_COMMITED);
                if (!opGuard.compareAndSet(null, opF)) {
                    throw new IllegalStateException();
                }
                return origin.send(event)
                        .thenCompose(unused -> opF)
                        .whenComplete((v, ex) -> opGuard.compareAndSet(opF, null))
                        .thenApply(unused -> new ResponseOpImpl())
                        ;
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            private CompletionStage<Void> responseCreate() {
                final var event = new OmniRealtimeResponseCreateClientEvent(StringUtils.uuid());
                final var opF = new OpFuture(OpFuture.State.WAITING_RESPONSE_CREATED);
                if (!opGuard.compareAndSet(null, opF)) {
                    throw new IllegalStateException();
                }
                return origin.send(event)
                        .thenCompose(unused -> opF)
                        .whenComplete((v, ex) -> opGuard.compareAndSet(opF, null))
                        ;
            }


            @Override
            public CompletableFuture<Void> create() {
                return null;
            }

        }

    }

}
