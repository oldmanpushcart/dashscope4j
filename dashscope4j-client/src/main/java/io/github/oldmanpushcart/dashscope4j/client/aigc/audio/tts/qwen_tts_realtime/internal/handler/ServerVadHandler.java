package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.BufferAppendTextClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
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
        final var serverVad = new ServerVadImpl((QwenTtsRealtimeEmitter) emitter);
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
            implements QwenTtsRealtimeEmitter.ServerVad {

        private final QwenTtsRealtimeEmitter delegate;

        private ServerVadImpl(QwenTtsRealtimeEmitter delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public ServerVad text(String text) {
            data(new BufferAppendTextClientEvent(genUUID22(), text));
            return this;
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return delegate.session();
        }

    }

}
