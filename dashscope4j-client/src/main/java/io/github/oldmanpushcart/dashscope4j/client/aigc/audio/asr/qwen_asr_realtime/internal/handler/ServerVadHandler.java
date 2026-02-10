package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.BufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
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
        delegate.onOpen(new ServerVadImpl((QwenAsrRealtimeEmitter) emitter));
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

    private static class ServerVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenAsrRealtimeEmitter.ServerVad {

        private final QwenAsrRealtimeEmitter delegate;

        private ServerVadImpl(QwenAsrRealtimeEmitter delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<Void> audio(ByteBuffer buffer) {
            final var event = new BufferAppendAudioClientEvent(genUUID22(), buffer);
            return data(event);
        }

        @Override
        public QwenAsrRealtimeSession session() {
            return delegate.session();
        }


    }

}
