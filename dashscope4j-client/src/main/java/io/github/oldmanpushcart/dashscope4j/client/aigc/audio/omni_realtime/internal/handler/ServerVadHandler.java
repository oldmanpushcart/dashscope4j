package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.BufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.BufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

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
    public void onData(ServerEvent output) {
        delegate.onData(output);
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        delegate.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        delegate.onClosed(ex);
    }

    private record ServerVadImpl(OmniRealtimeEmitter origin) implements ServerVad {

        @Override
        public ServerVad image(ByteBuffer image) {
            origin.data(new BufferAppendImageClientEvent(genUUID22(), image));
            return this;
        }

        @Override
        public ServerVad audio(ByteBuffer buffer) {
            origin.data(new BufferAppendAudioClientEvent(genUUID22(), buffer));
            return this;
        }

        @Override
        public OmniRealtimeSession session() {
            return origin.session();
        }

        @Override
        public void data(ClientEvent input) {
            origin.data(input);
        }

        @Override
        public void binary(ByteBuffer buffer) {
            origin.binary(buffer);
        }

        @Override
        public void close() {
            origin.close();
        }

        @Override
        public void close(Throwable ex) {
            origin.close(ex);
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
        public CompletionStage<Void> closeFuture() {
            return origin.closeFuture();
        }
    }

}
