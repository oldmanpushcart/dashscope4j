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
