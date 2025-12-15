package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferClearClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class OmniRealtimeManualExchangeImpl implements OmniRealtimeExchange.Manual {

    private final OmniRealtimeExchange origin;

    OmniRealtimeManualExchangeImpl(OmniRealtimeExchange origin) {
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
    public CompletionStage<Void> send(OmniRealtimeClientEvent event) {
        return origin.send(event);
    }

    @Override
    public CompletionStage<Void> send(ByteBuffer buffer) {
        return origin.send(buffer);
    }

    private String genEventId() {
        return UUID.randomUUID().toString();
    }



}
