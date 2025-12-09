package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

class OmniRealtimeExchangeImpl implements OmniRealtimeExchange {

    private final Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin;
    private final CompletableFuture<AtomicReference<OmniRealtimeSession>> sessionRefFuture = new CompletableFuture<>();
    private final SessionOp sessionOp = new SessionOpImpl();
    private final BufferOp bufferOp = new BufferOpImpl();
    private final ResponseOp responseOp = new ResponseOpImpl();

    public OmniRealtimeExchangeImpl(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin) {
        this.origin = origin;
    }

    public CompletableFuture<AtomicReference<OmniRealtimeSession>> getSessionRefFuture() {
        return sessionRefFuture;
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
    public CompletionStage<Void> send(OmniRealtimeClientEvent event) {
        return origin.send(event);
    }

    @Override
    public CompletionStage<Void> send(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public SessionOp session() {
        return sessionOp;
    }

    @Override
    public ResponseOp response() {
        return responseOp;
    }

    @Override
    public BufferOp buffer() {
        return bufferOp;
    }

    private String genEventId() {
        return UUID.randomUUID().toString();
    }

    private class SessionOpImpl implements SessionOp {

        @Override
        public CompletionStage<OmniRealtimeSession> get() {
            return sessionRefFuture
                    .thenApply(AtomicReference::get);
        }

        @Override
        public CompletionStage<Void> update(Parameters parameters) {
            final var session = new OmniRealtimeSession(parameters);
            return origin.send(new OmniRealtimeSessionUpdateClientEvent(genEventId(), session));
        }

    }

    private class BufferOpImpl implements BufferOp {

        @Override
        public CompletionStage<Void> appendImage(BufferedImage image) {
            return origin.send(new OmniRealtimeBufferAppendImageClientEvent(genEventId(), image));
        }

        @Override
        public CompletionStage<Void> appendAudio(ByteBuffer buffer) {
            return origin.send(new OmniRealtimeBufferAppendAudioClientEvent(genEventId(), buffer));
        }

        @Override
        public CompletionStage<Void> appendAudio(byte[] bytes, int offset, int length) {
            final var buffer = ByteBuffer.wrap(bytes, offset, length);
            return appendAudio(buffer);
        }

        @Override
        public CompletionStage<Void> commit() {
            return origin.send(new OmniRealtimeBufferCommitClientEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> clear() {
            return origin.send(new OmniRealtimeBufferClearClientEvent(genEventId()));
        }
    }

    private class ResponseOpImpl implements ResponseOp {

        @Override
        public CompletionStage<Void> create() {
            return origin.send(new OmniRealtimeResponseCreateClientEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> cancel() {
            return origin.send(new OmniRealtimeResponseCancelClientEvent(genEventId()));
        }

    }

}
