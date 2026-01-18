package io.github.oldmanpushcart.dashscope4j.client.internal.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

class ServerVadHandler implements OmniRealtimeExchange.Handler {

    private final OmniRealtimeExchange.Handler delegate;
    private final CompletableFuture<ServerVad> completeF = new CompletableFuture<>();

    public ServerVadHandler(OmniRealtimeExchange.Handler delegate) {
        this.delegate = delegate;
    }

    public CompletionStage<ServerVad> completeStage() {
        return completeF;
    }

    @Override
    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
        final var serverVad = new ServerVadImpl((OmniRealtimeExchange) exchange);
        delegate.onOpen(serverVad);
        completeF.complete(serverVad);
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {
        return delegate.onData(data);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return delegate.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        delegate.onClosed(ex);
        completeF.completeExceptionally(ex);
    }

    private static class ServerVadImpl extends Exchange.Proxy<OmniRealtimeClientEvent> implements ServerVad {

        private final OmniRealtimeExchange origin;

        private ServerVadImpl(OmniRealtimeExchange origin) {
            super(origin);
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> image(BufferedImage image) {
            final var event = new OmniRealtimeBufferAppendImageClientEvent(genUUID22(), image);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(ByteBuffer buffer) {
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
            final var buffer = ByteBuffer.wrap(bytes, offset, length);
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.send(event);
        }

        @Override
        public OmniRealtimeSession session() {
            return origin.session();
        }
    }

}
