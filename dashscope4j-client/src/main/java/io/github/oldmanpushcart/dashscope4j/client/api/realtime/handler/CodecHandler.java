package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CodecHandler<I, O, UI, UO> implements Realtime.Handler<I, O> {

    private final Function<UI, I> encoder;
    private final Function<O, UO> decoder;
    private final Realtime.Handler<UI, UO> next;

    public CodecHandler(Function<UI, I> encoder, Function<O, UO> decoder, Realtime.Handler<UI, UO> next) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.next = next;
    }

    @Override
    public void onOpen(Realtime.Emitter<I> emitter) {
        next.onOpen(new MapEmitter<>(encoder, emitter));
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

    private record MapEmitter<I, UI>(
            Function<UI, I> mapper,
            Realtime.Emitter<I> delegate
    ) implements Realtime.Emitter<UI> {


        @Override
        public CompletionStage<Void> data(UI input) {
            return delegate.data(mapper.apply(input));
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

    }

    public static <I,O> CodecHandler<String,String, I,O> json(Class<I> inType, Class<O> outType, Realtime.Handler<I,O> handler) {
        return new CodecHandler<>(
                JacksonJsonUtils::toJson,
                s -> JacksonJsonUtils.toObject(s, outType),
                handler
        );
    }

}
