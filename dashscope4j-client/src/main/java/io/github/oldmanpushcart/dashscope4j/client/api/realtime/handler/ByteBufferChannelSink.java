package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.CompletionStage;

public class ByteBufferChannelSink<T,R> implements Realtime.Handler<T,R> {

    private final WritableByteChannel channel;

    public ByteBufferChannelSink(WritableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public void onOpen(Realtime.Emitter<T> emitter) {

    }

    @Override
    public CompletionStage<Void> onData(R output) {
        return null;
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return null;
    }

    @Override
    public void onClosed(Throwable ex) {

    }

}
