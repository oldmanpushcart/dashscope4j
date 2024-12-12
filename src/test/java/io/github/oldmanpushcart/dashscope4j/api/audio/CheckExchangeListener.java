package io.github.oldmanpushcart.dashscope4j.api.audio;

import io.github.oldmanpushcart.dashscope4j.Exchange;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CheckExchangeListener<T, R> implements Exchange.Listener<T, R> {

    private final CompletableFuture<?> completeF = new CompletableFuture<>();
    private final AtomicInteger dataCntRef = new AtomicInteger(0);
    private final AtomicInteger byteCntRef = new AtomicInteger(0);
    private final List<R> items = new ArrayList<>();
    private final AtomicLong bytesRef = new AtomicLong(0L);

    @Override
    public void onOpen(Exchange<T> exchange) {
        Exchange.Listener.super.onOpen(exchange);
    }

    @Override
    public void onData(R data) {
        dataCntRef.incrementAndGet();
        items.add(data);
    }

    @Override
    public void onByteBuffer(ByteBuffer buf) {
        byteCntRef.incrementAndGet();
        bytesRef.addAndGet(buf.remaining());
    }

    @Override
    public void onCompleted() {
        completeF.complete(null);
    }

    @Override
    public void onError(Throwable ex) {
        completeF.completeExceptionally(ex);
    }

    public int dataCnt() {
        return dataCntRef.get();
    }

    public int byteCnt() {
        return byteCntRef.get();
    }

    public long bytes() {
        return bytesRef.get();
    }

    public List<R> items() {
        return Collections.unmodifiableList(items);
    }

    public CompletionStage<?> completeF() {
        return completeF;
    }

}
