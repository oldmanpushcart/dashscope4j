package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CodecHandler<I,O,UI,UO> implements Realtime.Handler<I,O> {

    private final Function<UI, I> encoder;
    private final Function<O, UO> decoder;
    private final Realtime.Handler<UI,UO> next;

    public CodecHandler(Function<UI, I> encoder, Function<O, UO> decoder, Realtime.Handler<UI, UO> next) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.next = next;
    }

    @Override
    public void onOpen(Realtime.Emitter<I> emitter) {
        next.onOpen(new Realtime.MapEmitter<>(encoder, emitter));
    }

    @Override
    public CompletionStage<Void> onData(O output) {
        try {
            return next.onData(decoder.apply(output));
        } catch (Exception e) {
            return CompletableFuture.failedStage(e);
        }
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return next.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        next.onClosed(ex);
    }

}
