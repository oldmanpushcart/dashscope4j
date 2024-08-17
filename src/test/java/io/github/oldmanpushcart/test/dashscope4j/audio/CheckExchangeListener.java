package io.github.oldmanpushcart.test.dashscope4j.audio;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckExchangeListener<T, R> implements Exchange.Listener<T, R> {

    private final CompletableFuture<Void> completeF = new CompletableFuture<>();
    private final AtomicInteger dataCntRef = new AtomicInteger(0);
    private final AtomicInteger byteCntRef = new AtomicInteger(0);
    private final List<R> items = new ArrayList<>();

    @Override
    public CompletableFuture<?> onData(Exchange<T, R> exchange, R data) {
        dataCntRef.incrementAndGet();
        items.add(data);
        return Exchange.Listener.super.onData(exchange, data);
    }

    @Override
    public CompletableFuture<?> onByteBuffer(Exchange<T, R> exchange, ByteBuffer buf, boolean last) {
        byteCntRef.incrementAndGet();
        return Exchange.Listener.super.onByteBuffer(exchange, buf, last);
    }

    @Override
    public CompletableFuture<?> onCompleted(Exchange<T, R> exchange, int status, String reason) {
        completeF.complete(null);
        return Exchange.Listener.super.onCompleted(exchange, status, reason);
    }

    @Override
    public void onError(Exchange<T, R> exchange, Throwable ex) {
        completeF.completeExceptionally(ex);
        Exchange.Listener.super.onError(exchange, ex);
    }

    public CompletableFuture<Void> getCompleteFuture() {
        return completeF;
    }

    public int getDataCnt() {
        return dataCntRef.get();
    }

    public int getByteCnt() {
        return byteCntRef.get();
    }

    public List<R> getItems() {
        return items;
    }

}
