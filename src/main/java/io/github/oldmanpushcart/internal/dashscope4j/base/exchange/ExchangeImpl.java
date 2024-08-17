package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

class ExchangeImpl<T, R> implements Exchange<T, R> {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final WebSocket socket;
    private final String uuid;
    private final Exchange.Mode mode;
    private final Function<T, String> encoder;
    private final AtomicBoolean isFirstRef = new AtomicBoolean(true);

    public ExchangeImpl(WebSocket socket, String uuid, Mode mode, Function<T, String> encoder) {
        this.socket = socket;
        this.uuid = uuid;
        this.mode = mode;
        this.encoder = encoder;
    }

    private CompletableFuture<Exchange<T, R>> sendText(String text) {
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
    public CompletableFuture<Exchange<T, R>> write(T data) {

        final var type = isFirstRef.get() && isFirstRef.compareAndSet(true, false)
                ? InFrame.Type.RUN
                : InFrame.Type.CONTINUE;

        final var frame = InFrame.of(uuid, type, mode, encoder.apply(data));
        final var encoded = JacksonUtils.toJson(frame);
        return sendText(encoded);
    }

    @Override
    public CompletableFuture<Exchange<T, R>> write(ByteBuffer buf, boolean last) {
        final var remaining = buf.remaining();
        return socket.sendBinary(buf, last)
                .thenApply(v -> {
                    logger.trace("WEBSOCKET: >>> BINARY;last={};bytes={};", last, remaining);
                    return this;
                });
    }

    @Override
    public CompletableFuture<Exchange<T, R>> write(ByteBuffer buf) {
        return write(buf, true);
    }

    @Override
    public void request(long n) {
        socket.request(n);
    }

    @Override
    public CompletableFuture<Exchange<T, R>> finishing() {
        final var frame = InFrame.of(uuid, InFrame.Type.FINISH, mode, "{\"input\": {}}");
        final var encoded = JacksonUtils.toJson(frame);
        return sendText(encoded);
    }


    @Override
    public CompletableFuture<Exchange<T, R>> close(int status, String reason) {
        return socket.sendClose(status, reason)
                .thenApply(v -> {
                    logger.trace("WEBSOCKET: >>> CLOSE;status={};reason={};", status, reason);
                    return this;
                });
    }

    @Override
    public void abort() {
        socket.abort();
        logger.trace("WEBSOCKET: >>> ABORT;");
    }

}
