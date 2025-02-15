package io.github.oldmanpushcart.dashscope4j;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 代理Exchange
 *
 * @param <T> 数据类型
 * @since 3.1.0
 */
public class DelegateExchange<T> implements Exchange<T> {

    private final Exchange<T> delegate;

    public DelegateExchange(Exchange<T> delegate) {
        Objects.requireNonNull(delegate);
        this.delegate = delegate;
    }

    @Override
    public String uuid() {
        return delegate.uuid();
    }

    @Override
    public Mode mode() {
        return delegate.mode();
    }
    
    @Override
    public boolean write(T data) {
        //noinspection deprecation
        return delegate.write(data);
    }

    @Override
    public boolean writeData(T data) {
        return delegate.writeData(data);
    }

    @Override
    public boolean write(ByteBuffer buf) {
        //noinspection deprecation
        return delegate.write(buf);
    }

    @Override
    public boolean writeByteBuffer(ByteBuffer buf) {
        return delegate.writeByteBuffer(buf);
    }

    @Override
    public Disposable subscribeForWriteData(Flowable<T> flow, boolean finishingAfterWrite) {
        return delegate.subscribeForWriteData(flow, finishingAfterWrite);
    }

    @Override
    public Disposable subscribeForWriteByteBuffer(Flowable<ByteBuffer> flow, boolean finishingAfterWrite) {
        return delegate.subscribeForWriteByteBuffer(flow, finishingAfterWrite);
    }

    @Override
    public boolean finishing() {
        return delegate.finishing();
    }

    @Override
    public boolean closing(int status, String reason) {
        return delegate.closing(status, reason);
    }

    @Override
    public boolean closing(Throwable ex) {
        return delegate.closing(ex);
    }

    @Override
    public boolean closing() {
        return delegate.closing();
    }

    @Override
    public void abort() {
        delegate.abort();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public CompletionStage<?> closeStage() {
        return delegate.closeStage();
    }

    /**
     * 代理Exchange.Listener
     *
     * @param <T> 流入数据类型
     * @param <R> 流出数据类型
     */
    public static class Listener<T, R> implements Exchange.Listener<T, R> {

        private final Exchange.Listener<T, R> delegate;

        public Listener(Exchange.Listener<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onOpen(Exchange<T> exchange) {
            delegate.onOpen(exchange);
        }

        @Override
        public void onData(R data) {
            delegate.onData(data);
        }

        @Override
        public void onByteBuffer(ByteBuffer buf) {
            delegate.onByteBuffer(buf);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }

        @Override
        public void onError(Throwable ex) {
            delegate.onError(ex);
        }

    }

}
