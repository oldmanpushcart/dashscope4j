package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class OmniRealtimeExchangeImpl implements OmniRealtimeExchange {

    private final Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin;
    private final AtomicReference<Parameters> parametersRef = new AtomicReference<>();
    private final ParametersOp parametersOp = new ParametersOpImpl();
    private final BufferOp bufferOp = new BufferOpImpl();
    private final ResponseOp responseOp = new ResponseOpImpl();

    public OmniRealtimeExchangeImpl(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin) {
        this.origin = origin;
    }

    void updateParameters(Parameters parameters) {
        parametersRef.set(parameters);
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
    public ParametersOp parameters() {
        return parametersOp;
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

    private class ParametersOpImpl implements ParametersOp {

        @Override
        public Parameters get() {
            return parametersRef.get();
        }

        @Override
        public CompletionStage<Void> update(Parameters parameters) {
            return origin.send(new OmniRealtimeSessionUpdateClientEvent(
                    genEventId(),
                    parameters
            ));
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
