package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class ProxyExchangeListener<T, R> implements Exchange.Listener<T, R> {

    private final Exchange.Listener<T, R> target;

    public ProxyExchangeListener(Exchange.Listener<T, R> target) {
        this.target = target;
    }


    @Override
    public void onOpen(Exchange<T, R> exchange) {
        target.onOpen(exchange);
    }

    @Override
    public CompletableFuture<?> onData(Exchange<T, R> exchange, R data) {
        return target.onData(exchange, data);
    }

    @Override
    public CompletableFuture<?> onByteBuffer(Exchange<T, R> exchange, ByteBuffer buf, boolean last) {
        return target.onByteBuffer(exchange, buf, last);
    }

    @Override
    public CompletableFuture<?> onCompleted(Exchange<T, R> exchange, int status, String reason) {
        return target.onCompleted(exchange, status, reason);
    }

    @Override
    public void onError(Exchange<T, R> exchange, Throwable ex) {
        target.onError(exchange, ex);
    }

}
