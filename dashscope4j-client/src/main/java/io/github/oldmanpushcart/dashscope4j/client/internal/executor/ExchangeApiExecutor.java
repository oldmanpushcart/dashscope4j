package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class ExchangeApiExecutor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String ak;
    private final HttpClient http;

    public ExchangeApiExecutor(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    public <T, R> CompletionStage<Exchange<T, R>> newExchange(
            final URI endpoint,
            final Exchange.Codec<T, R> codec,
            final Exchange.Handler<T, R> handler
    ) {
        final var exchangeFutureListener = new ExchangeFutureListener<>(endpoint, codec, handler);
        return http.newWebSocketBuilder()
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .buildAsync(endpoint, exchangeFutureListener)
                .thenCompose(ws -> exchangeFutureListener.getFuture());
    }


    private static class ExchangeImpl<T, R> implements Exchange<T, R> {

        private final WebSocket ws;
        private final Exchange.Codec<T, R> codec;

        private ExchangeImpl(WebSocket ws, Codec<T, R> codec) {
            this.ws = ws;
            this.codec = codec;
        }

        @Override
        public boolean isClosed() {
            return ws.isInputClosed() || ws.isOutputClosed();
        }

        @Override
        public CompletionStage<Void> closing() {
            return ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye!")
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public void close() {
            ws.abort();
        }

        @Override
        public CompletionStage<Void> send(T data) {
            final var body = codec.encode(data);
            return ws.sendText(body, true)
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            return ws.sendBinary(buffer, true)
                    .thenAccept(unused -> {
                    });
        }

    }

    private static class ExchangeFutureListener<T, R> implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final URI endpoint;
        private final Exchange.Codec<T, R> codec;
        private final Exchange.Handler<T, R> handler;

        private final CompletableFuture<Exchange<T, R>> exchangeF = new CompletableFuture<>();
        private final StringBuilder stringBuf = new StringBuilder();
        private final AtomicBoolean errorFlag = new AtomicBoolean(false);

        private ExchangeFutureListener(
                final URI endpoint,
                final Exchange.Codec<T, R> codec,
                final Exchange.Handler<T, R> handler
        ) {
            this.endpoint = endpoint;
            this.codec = codec;
            this.handler = handler;
        }

        public CompletionStage<Exchange<T,R>> getFuture() {
            return exchangeF;
        }

        @Override
        public void onOpen(WebSocket ws) {

            logger.trace("dashscope-client://exchange {} opened.", endpoint);

            try {
                final var exchange = new ExchangeImpl<>(ws, codec);
                handler.onOpen(exchange);
                exchangeF.complete(exchange);
                ws.request(1);
            } catch (Throwable ex) {
                ws.abort();
                fireClosed(ex);
            }
        }

        private CompletionStage<Void> fireClosed(Throwable ex) {

            if (!errorFlag.compareAndSet(false, true)) {
                return CompletableFuture.completedStage(null);
            }

            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> handler.onClosed(ex))
                    .exceptionally(closeEx -> {
                        logger.trace("dashscope-client://exchange {} occur error when fire closed!", endpoint, closeEx);
                        return null;
                    });
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence string, boolean last) {

            stringBuf.append(string);
            if (!last) {
                ws.request(1);
                return null;
            }

            final var body = stringBuf.toString();
            stringBuf.setLength(0);

            return CompletableFuture.completedStage(body)
                    .thenApply(codec::decode)
                    .thenCompose(handler::onData)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            ws.abort();
                            fireClosed(ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            return CompletableFuture.completedStage(data)
                    .thenCompose(handler::onBinary)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            ws.abort();
                            fireClosed(ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            return ws.sendPong(message)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            ws.abort();
                            fireClosed(ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            return WebSocket.Listener.super.onPong(ws, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            final var ex = status == WebSocket.NORMAL_CLOSURE
                    ? null
                    : new WebSocketCloseException(status, reason);
            return fireClosed(ex);
        }

        @Override
        public void onError(WebSocket ws, Throwable ex) {
            fireClosed(ex);
        }

    }

    /**
     * 连接
     *
     * @param ws       websocket
     * @param listener exchange listener
     */
    private record Connection(WebSocket ws, Exchange.Handler<?, ?> listener) {

    }

    /**
     * WebSocket关闭异常
     */
    private static class WebSocketCloseException extends Exception {

        private WebSocketCloseException(int status, String reason) {
            super("WebSocket closed with status %d: %s".formatted(
                    status,
                    reason != null ? reason : "(no reason)"
            ));
        }

    }

}
