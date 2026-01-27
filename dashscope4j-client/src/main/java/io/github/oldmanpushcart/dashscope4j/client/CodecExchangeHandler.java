package io.github.oldmanpushcart.dashscope4j.client;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class CodecExchangeHandler<T, R, UT, UR> implements Exchange.Handler<T, R> {

    private final Function<UT, T> encoder;
    private final Function<R, UR> decoder;
    private final Exchange.Handler<UT, UR> next;

    public CodecExchangeHandler(Function<UT, T> encoder, Function<R, UR> decoder, Exchange.Handler<UT, UR> next) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.next = next;
    }

    @Override
    public void onOpen(Exchange<T> exchange) {
        next.onOpen(new Exchange<UT>() {
            @Override
            public String id() {
                return exchange.id();
            }

            @Override
            public boolean isClosed() {
                return exchange.isClosed();
            }

            @Override
            public void close() {
                exchange.close();
            }

            @Override
            public CompletionStage<Void> closeFuture() {
                return exchange.closeFuture();
            }

            @Override
            public CompletionStage<Void> closing() {
                return exchange.closing();
            }

            @Override
            public CompletionStage<Void> send(UT data) {
                try {
                    return exchange.send(encoder.apply(data));
                } catch (Exception e) {
                    return CompletableFuture.failedStage(e);
                }
            }

            @Override
            public CompletionStage<Void> send(ByteBuffer buffer) {
                return exchange.send(buffer);
            }
        });
    }

    @Override
    public CompletionStage<Void> onData(R data) {
        try {
            return next.onData(decoder.apply(data));
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
