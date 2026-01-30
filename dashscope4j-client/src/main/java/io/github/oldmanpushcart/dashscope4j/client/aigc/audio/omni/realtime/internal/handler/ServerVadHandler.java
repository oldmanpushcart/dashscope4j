package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.OmniRealtimeEmitter.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni.realtime.event.server.OmniRealtimeServerEvent;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ServerVadHandler implements Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    private final Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> delegate;

    public ServerVadHandler(Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<OmniRealtimeClientEvent> emitter) {
        final var serverVad = new ServerVadImpl((OmniRealtimeEmitter) emitter);
        delegate.onOpen(serverVad);
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent output) {
        return delegate.onData(output);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return delegate.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        delegate.onClosed(ex);
    }

    private static class ServerVadImpl implements ServerVad {

        private final OmniRealtimeEmitter origin;

        private ServerVadImpl(OmniRealtimeEmitter origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> image(BufferedImage image) {
            final var event = new OmniRealtimeBufferAppendImageClientEvent(genUUID22(), image);
            return origin.emit(event);
        }

        @Override
        public CompletionStage<Void> audio(ByteBuffer buffer) {
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.emit(event);
        }

        @Override
        public CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
            final var buffer = ByteBuffer.wrap(bytes, offset, length);
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.emit(event);
        }

        @Override
        public OmniRealtimeSession session() {
            return origin.session();
        }

        @Override
        public CompletionStage<Void> emit(OmniRealtimeClientEvent input) {
            return origin.emit(input);
        }

        @Override
        public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
            return origin.emitBinary(buffer);
        }

        @Override
        public CompletionStage<Void> emitClose() {
            return origin.emitClose();
        }

        @Override
        public CompletionStage<Void> emitClose(Throwable ex) {
            return origin.emitClose(ex);
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
        public void close() {
            origin.close();
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return origin.closeFuture();
        }
    }

}
