package io.github.oldmanpushcart.dashscope4j.client.realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CodecHandler<I,O,UI,UO> implements Realtime.Handler<I,O> {

    private final Function<UO, O> encoder;
    private final Function<I, UI> decoder;
    private final Realtime.Handler<UI,UO> next;

    public CodecHandler(Function<UO, O> encoder, Function<I, UI> decoder, Realtime.Handler<UI, UO> next) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.next = next;
    }


    @Override
    public void onOpen(Realtime.Emitter<O> emitter) {
        next.onOpen(new Realtime.Emitter<UO>() {
            @Override
            public CompletionStage<Void> emit(UO output) {
                return emitter.emit(encoder.apply(output));
            }

            @Override
            public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
                return emitter.emitBinary(buffer);
            }

            @Override
            public CompletionStage<Void> emitClose() {
                return emitter.emitClose();
            }

            @Override
            public CompletionStage<Void> emitClose(Throwable ex) {
                return emitter.emitClose(ex);
            }

            @Override
            public String id() {
                return emitter.id();
            }

            @Override
            public boolean isClosed() {
                return emitter.isClosed();
            }

            @Override
            public void close() {
                emitter.close();
            }

            @Override
            public CompletionStage<Void> closeFuture() {
                return emitter.closeFuture();
            }
        });
    }

    @Override
    public CompletionStage<Void> onData(I input) {
        try {
            return next.onData(decoder.apply(input));
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
