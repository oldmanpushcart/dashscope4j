package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

class ExchangeImpl<T, R> implements Exchange<T, R> {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final WebSocket socket;
    private final String uuid;
    private final Exchange.Mode mode;
    private final CompletableFuture<?> closeF;
    private final Function<T, String> encoder;
    private final AtomicBoolean isFirstRef = new AtomicBoolean(true);


    public ExchangeImpl(WebSocket socket, String uuid, Mode mode, CompletableFuture<?> closeF, Function<T, String> encoder) {
        this.socket = socket;
        this.uuid = uuid;
        this.mode = mode;
        this.closeF = closeF;
        this.encoder = encoder;
    }

    private CompletionStage<Exchange<T, R>> sendText(String text) {
        return socket.sendText(text, true)
                .thenApply(v -> {
                    logger.trace("WEBSOCKET: >>> TEXT;last={};text={};", true, text);
                    return this;
                });
    }

    @Override
    public String uuid() {
        return uuid;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeData(T data) {

        final var type = isFirstRef.get() && isFirstRef.compareAndSet(true, false)
                ? InFrame.Type.RUN
                : InFrame.Type.CONTINUE;

        final var frame = InFrame.of(uuid, type, mode, encoder.apply(data));
        final var encoded = JacksonUtils.toJson(frame);
        return sendText(encoded);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeDataPublisher(Flow.Publisher<T> publisher) {
        final var future = new CompletableFuture<Exchange<T, R>>();
        publisher.subscribe(new Flow.Subscriber<>() {

            private volatile Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(T item) {
                writeData(item).whenComplete((v, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        subscription.request(1);
                    }
                });
            }

            @Override
            public void onError(Throwable ex) {
                future.completeExceptionally(ex);
            }

            @Override
            public void onComplete() {
                future.complete(ExchangeImpl.this);
            }
        });
        return future;
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBuffer(ByteBuffer buf, boolean last) {
        final var remaining = buf.remaining();
        return socket.sendBinary(buf, last)
                .thenApply(v -> {
                    logger.trace("WEBSOCKET: >>> BINARY;last={};bytes={};", last, remaining);
                    return this;
                });
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBuffer(ByteBuffer buf) {
        return writeByteBuffer(buf, true);
    }

    @Override
    public CompletionStage<Exchange<T, R>> writeByteBufferPublisher(Flow.Publisher<ByteBuffer> publisher) {
        final var future = new CompletableFuture<Exchange<T, R>>();
        publisher.subscribe(new Flow.Subscriber<>() {

            private volatile Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(ByteBuffer buf) {
                writeByteBuffer(buf).whenComplete((v, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        subscription.request(1);
                    }
                });
            }

            @Override
            public void onError(Throwable ex) {
                future.completeExceptionally(ex);
            }

            @Override
            public void onComplete() {
                future.complete(ExchangeImpl.this);
            }
        });
        return future;
    }

    @Override
    public void request(long n) {
        socket.request(n);
    }

    @Override
    public CompletionStage<Exchange<T, R>> finishing() {
        final var frame = InFrame.of(uuid, InFrame.Type.FINISH, mode, "{\"input\": {}}");
        final var encoded = JacksonUtils.toJson(frame);
        return sendText(encoded);
    }


    @Override
    public CompletionStage<Exchange<T, R>> closing(int status, String reason) {
        return socket.sendClose(status, reason)
                .thenApply(v -> {
                    logger.trace("WEBSOCKET: >>> CLOSE;status={};reason={};", status, reason);
                    return this;
                });
    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public CompletionStage<?> closeFuture() {
        return closeF;
    }

    @Override
    public void abort() {
        if (closeF.complete(null)) {
            socket.abort();
            logger.trace("WEBSOCKET: >>> ABORT;");
        }
    }

}
