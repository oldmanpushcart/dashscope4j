package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.BufferAppendTextClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.ServerEvent;
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
        final var serverVad = new ServerVadImpl((QwenTtsRealtimeEmitter) emitter);
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

    private record ServerVadImpl(QwenTtsRealtimeEmitter delegate) implements QwenTtsRealtimeEmitter.ServerVad {

        @Override
        public CompletionStage<Void> text(String text) {
            final var event = new BufferAppendTextClientEvent(genUUID22(), text);
            return delegate.data(event);
        }

        @Override
        public CompletionStage<Void> data(ClientEvent input) {
            return delegate.data(input);
        }

        @Override
        public CompletionStage<Void> binary(ByteBuffer buffer) {
            return delegate.binary(buffer);
        }

        @Override
        public CompletionStage<Void> closing() {
            return delegate.closing();
        }

        @Override
        public CompletionStage<Void> closing(Throwable ex) {
            return delegate.closing(ex);
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return delegate.closeFuture();
        }

        @Override
        public QwenTtsRealtimeSession session() {
            return delegate.session();
        }

    }

}
