package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.BufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.BufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ServerVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    public ServerVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var serverVad = new ServerVadImpl((OmniRealtimeEmitter) emitter);
        delegate.onOpen(serverVad);
    }

    @Override
    public CompletionStage<Void> onData(ServerEvent output) {
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

    private record ServerVadImpl(OmniRealtimeEmitter origin) implements ServerVad {

        @Override
            public CompletionStage<Void> image(BufferedImage image) {
                final var event = new BufferAppendImageClientEvent(genUUID22(), image);
                return origin.data(event);
            }

            @Override
            public CompletionStage<Void> audio(ByteBuffer buffer) {
                final var event = new BufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.data(event);
            }

            @Override
            public CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
                final var buffer = ByteBuffer.wrap(bytes, offset, length);
                final var event = new BufferAppendAudioClientEvent(genUUID22(), buffer);
                return origin.data(event);
            }

            @Override
            public OmniRealtimeSession session() {
                return origin.session();
            }

            @Override
            public CompletionStage<Void> data(ClientEvent input) {
                return origin.data(input);
            }

            @Override
            public CompletionStage<Void> binary(ByteBuffer buffer) {
                return origin.binary(buffer);
            }

            @Override
            public CompletionStage<Void> closing() {
                return origin.closing();
            }

            @Override
            public CompletionStage<Void> closing(Throwable ex) {
                return origin.closing(ex);
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
