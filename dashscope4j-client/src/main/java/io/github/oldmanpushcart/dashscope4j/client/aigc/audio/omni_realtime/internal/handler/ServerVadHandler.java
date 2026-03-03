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

    private static class ServerVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements ServerVad {

        private final OmniRealtimeEmitter origin;

        public ServerVadImpl(OmniRealtimeEmitter origin) {
            super(origin);
            this.origin = origin;
        }

        @Override
        public OmniRealtimeSession session() {
            return origin.session();
        }

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

    }

}
