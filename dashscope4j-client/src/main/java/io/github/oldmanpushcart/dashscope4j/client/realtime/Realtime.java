package io.github.oldmanpushcart.dashscope4j.client.realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface Realtime {

    interface Connection extends AutoCloseable{

        String id();
        boolean isClosed();

        @Override
        void close();

        CompletionStage<Void> closeFuture();

    }

    interface Emitter<O> extends Connection {

        CompletionStage<Void> emit(O output);

        CompletionStage<Void> emitBinary(ByteBuffer buffer);

        CompletionStage<Void> emitClose();

        CompletionStage<Void> emitClose(Throwable ex);

    }

    interface Handler<I, O> {

        void onOpen(Emitter<O> emitter);

        CompletionStage<Void> onData(I input);

        CompletionStage<Void> onBinary(ByteBuffer buffer);

        void onClosed(Throwable ex);

    }

}
