package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

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
    public CompletionStage<Exchange<T, R>> writeData(T data) {
        return target.writeData(data);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeDataPublisher(Flow.Publisher<T> publisher) {
        return target.writeDataPublisher(publisher);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBuffer(ByteBuffer buf, boolean last) {
        return target.writeByteBuffer(buf, last);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBuffer(ByteBuffer buf) {
        return target.writeByteBuffer(buf);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBufferPublisher(Flow.Publisher<ByteBuffer> publisher) {
        return target.writeByteBufferPublisher(publisher);
    }

    @Override
    public CompletionStage<Exchange<T, R>> finishing() {
        return target.finishing();
    }

    @Override
    public CompletionStage<Exchange<T, R>> closing(int status, String reason) {
        return target.closing(status, reason);
    }

    @Override
    public boolean isClosed() {
        return target.isClosed();
    }

    @Override
    public CompletionStage<?> closeFuture() {
        return target.closeFuture();
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
