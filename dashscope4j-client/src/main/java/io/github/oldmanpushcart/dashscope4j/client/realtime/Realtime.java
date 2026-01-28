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

        CompletionStage<?> emit(O output);

        CompletionStage<?> emitBinary(ByteBuffer buffer);

        CompletionStage<?> emitClose();

        CompletionStage<?> emitClose(Throwable ex);

    }

    interface Handler<I, O> {

        void onOpen(Emitter<O> emitter);

        CompletionStage<?> onData(I input);

        CompletionStage<?> onBinary(ByteBuffer buffer);

        void onClosed(Throwable ex);

    }

}
