package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface Realtime {

    interface Connection extends AutoCloseable {

        String id();

        boolean isClosed();

        @Override
        void close();

        CompletionStage<Void> closeFuture();

    }

    interface Emitter<I> extends Connection {

        CompletionStage<Void> emit(I input);

        CompletionStage<Void> emitBinary(ByteBuffer buffer);

        CompletionStage<Void> emitClose();

        CompletionStage<Void> emitClose(Throwable ex);

    }

    class DelegateEmitter<I> implements Emitter<I> {

        private final Emitter<I> delegate;

        public DelegateEmitter(Emitter<I> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<Void> emit(I input) {
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
    }

    interface Handler<I, O> {

        void onOpen(Emitter<I> emitter);

        CompletionStage<Void> onData(O output);

        CompletionStage<Void> onBinary(ByteBuffer buffer);

        void onClosed(Throwable ex);

    }

    interface Session<I, O> {

        Model<I, O> model();

        Function<Handler<I, O>, Handler<String, String>> provider();

    }

}
