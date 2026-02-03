package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.QwenTtsRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeBufferAppendTextClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ServerVadHandler implements Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> {

    private final Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> delegate;

    public ServerVadHandler(Realtime.Handler<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<QwenTtsRealtimeClientEvent> emitter) {
        final var serverVad = new ServerVadImpl((QwenTtsRealtimeEmitter) emitter);
        delegate.onOpen(serverVad);
    }

    @Override
    public CompletionStage<Void> onData(QwenTtsRealtimeServerEvent output) {
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
            final var event = new QwenTtsRealtimeBufferAppendTextClientEvent(genUUID22(), text);
            return delegate.emit(event);
        }

        @Override
        public CompletionStage<Void> emit(QwenTtsRealtimeClientEvent input) {
            return delegate.emit(input);
        }

        @Override
        public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
            return delegate.emitBinary(buffer);
        }

        @Override
        public CompletionStage<Void> emitClose() {
            return delegate.emitClose();
        }

        @Override
        public CompletionStage<Void> emitClose(Throwable ex) {
            return delegate.emitClose(ex);
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
