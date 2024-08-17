package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class ProxyExchange<T, R> implements Exchange<T, R> {

    private final Exchange<T, R> target;

    public ProxyExchange(Exchange<T, R> target) {
        this.target = target;
    }

    @Override
    public String uuid() {
        return target.uuid();
    }

    @Override
    public Mode mode() {
        return target.mode();
    }

    @Override
    public CompletableFuture<Exchange<T, R>> write(T data) {
        return target.write(data);
    }

    @Override
    public CompletableFuture<Exchange<T, R>> write(ByteBuffer buf, boolean last) {
        return target.write(buf, last);
    }

    @Override
    public CompletableFuture<Exchange<T, R>> write(ByteBuffer buf) {
        return target.write(buf);
    }

    @Override
    public CompletableFuture<Exchange<T, R>> finishing() {
        return target.finishing();
    }

    @Override
    public CompletableFuture<Exchange<T, R>> close(int status, String reason) {
        return target.close(status, reason);
    }

    @Override
    public void abort() {
        target.abort();
    }

    @Override
    public void request(long n) {
        target.request(n);
    }

}
