package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface OmniRealtimeConversation {

    CompletionStage<OmniRealtimeConversation> open(Handler handler);

    boolean isClosed();

    CompletionStage<Void> close();

    CompletionStage<Void> config(Parameters parameters);

    ResponseOp response();

    BufferOp buffer();

    interface ResponseOp {

        CompletionStage<Void> create();

        CompletionStage<Void> cancel();

    }

    interface BufferOp {

        CompletionStage<Void> append(BufferedImage image);

        CompletionStage<Void> append(ByteBuffer buffer);

        CompletionStage<Void> commit();

        CompletionStage<Void> clear();

    }

    interface Handler {

        void onOpen();

        CompletionStage<Void> onData(String json);

        CompletionStage<Void> onClosed(Throwable ex);

    }

}
