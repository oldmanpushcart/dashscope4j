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

        CompletionStage<Void> data(I input);

        CompletionStage<Void> binary(ByteBuffer buffer);

        CompletionStage<Void> closing();

        CompletionStage<Void> closing(Throwable ex);

    }

    class DelegateEmitter<I> implements Emitter<I> {

        private final Emitter<I> delegate;

        public DelegateEmitter(Emitter<I> delegate) {
            this.delegate = delegate;
        }

        @Override
        public CompletionStage<Void> data(I input) {
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
