package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeConversation;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.event.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeConversationImpl implements OmniRealtimeConversation {

    private final Exchange<OmniRealtimeEvent, String> exchange;
    private final ResponseOp responseOp = new ResponseOpImpl();
    private final BufferOp bufferOp = new BufferOpImpl();

    public OmniRealtimeConversationImpl(Exchange<OmniRealtimeEvent, String> exchange) {
        this.exchange = exchange;
    }

    @Override
    public CompletionStage<OmniRealtimeConversation> open(Handler handler) {
        return exchange
                .open(new Exchange.Handler<>() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeEvent, String> exchange) {
                        handler.onOpen();
                    }

                    @Override
                    public CompletionStage<Void> onData(String data) {
                        return handler.onData(data);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        return handler.onClosed(ex);
                    }

                })
                .thenApply(v -> this);
    }

    @Override
    public boolean isClosed() {
        return exchange.isClosed();
    }

    @Override
    public CompletionStage<Void> close() {
        return exchange.close();
    }

    private String genEventId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public CompletionStage<Void> config(Parameters parameters) {
        return exchange.send(new OmniRealtimeSessionUpdateEvent(genEventId(), parameters));
    }

    @Override
    public ResponseOp response() {
        return responseOp;
    }

    @Override
    public BufferOp buffer() {
        return bufferOp;
    }

    private class ResponseOpImpl implements ResponseOp {

        @Override
        public CompletionStage<Void> create() {
            return exchange.send(new OmniRealtimeCreateResponseEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> cancel() {
            return exchange.send(new OmniRealtimeCancelResponseEvent(genEventId()));
        }

    }

    private class BufferOpImpl implements BufferOp {

        @Override
        public CompletionStage<Void> append(BufferedImage image) {
            return exchange.send(new OmniRealtimeInputImageBufferAppendEvent(genEventId(), image));
        }

        @Override
        public CompletionStage<Void> append(ByteBuffer buffer) {
            return exchange.send(new OmniRealtimeInputAudioBufferAppendEvent(genEventId(), buffer));
        }

        @Override
        public CompletionStage<Void> commit() {
            return exchange.send(new OmniRealtimeInputAudioBufferCommitEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> clear() {
            return exchange.send(new OmniRealtimeInputAudioBufferClearEvent(genEventId()));
        }

    }

}
