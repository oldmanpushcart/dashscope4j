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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class HttpWsExchangeExecutor {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String ak;
    private final HttpClient http;

    public HttpWsExchangeExecutor(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    public <T, R> Exchange<T, R> newExchange(
            final URI endpoint,
            final Function<T, String> encoder,
            final Function<String, R> decoder
    ) {

        return new Exchange<>() {

            private final AtomicReference<Connection> activeConnectionRef = new AtomicReference<>();

            @Override
            public CompletionStage<Exchange<T, R>> open(Handler<T, R> handler) {
                return http.newWebSocketBuilder()
                        .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                        .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                        .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                        .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                        .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                        .buildAsync(endpoint, new WsListenerImpl<>(decoder, this, handler))
                        .thenApply(ws -> {

                            /*
                             * 如果已经打开，
                             * 则本次打开操作失败，并关闭已经打开的websocket连接
                             */
                            if (!activeConnectionRef.compareAndSet(null, new Connection(ws, handler))) {
                                ws.abort();
                                throw new IllegalStateException("Exchange already opened!");
                            }

                            return this;
                        });
            }

            @Override
            public boolean isClosed() {
                return null == activeConnectionRef.get();
            }

            @Override
            public CompletionStage<Void> close() {
                final var connection = activeConnectionRef.getAndSet(null);
                if (null == connection) {
                    return CompletableFuture.completedStage(null);
                }
                return CompletableFuture.completedStage(null)
                        .thenCompose(unused -> connection.ws().sendClose(WebSocket.NORMAL_CLOSURE, "bye!"))
                        .thenAccept(unused -> {
                        });
            }

            private Connection requireActiveConnection() {
                final var connection = activeConnectionRef.get();
                if (null == connection) {
                    throw new IllegalStateException("exchange not opened");
                }
                return connection;
            }

            @Override
            public CompletionStage<Void> send(T data) {
                final var connection = requireActiveConnection();
                return CompletableFuture.completedStage(data)
                        .thenApply(encoder)
                        .thenCompose(body -> {
                            logger.trace("dashscope-client/exchange/send >>> {}", body);
                            return connection.ws().sendText(body, true);
                        })
                        .thenAccept(unused -> {
                        });
            }

            @Override
            public CompletionStage<Void> send(ByteBuffer buffer) {
                final var connection = requireActiveConnection();
                logger.trace("dashscope-client/exchange/send >>> bytes[{}]", buffer.capacity());
                return connection.ws().sendBinary(buffer, true)
                        .thenAccept(unused -> {
                        });
            }

        };
    }

    private static class WsListenerImpl<T, R> implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final Function<String, R> decoder;
        private final Exchange<T, R> exchange;
        private final Exchange.Handler<T, R> handler;
        private final StringBuilder stringBuf = new StringBuilder();
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private WsListenerImpl(
                final Function<String, R> decoder,
                final Exchange<T, R> exchange,
                final Exchange.Handler<T, R> handler
        ) {
            this.decoder = decoder;
            this.exchange = exchange;
            this.handler = handler;
        }

        @Override
        public void onOpen(WebSocket ws) {
            logger.trace("dashscope-client/exchange/handler/open");
            handler.onOpen(exchange);
            ws.request(1);
        }

        private CompletionStage<Void> fireClosed(Throwable ex) {
            if (!closed.compareAndSet(false, true)) {
                return CompletableFuture.completedStage(null);
            }
            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> handler.onClosed(ex))
                    .exceptionally(closeEx -> {
                        // TODO : log the exception
                        return null;
                    });
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence string, boolean last) {

            stringBuf.append(string);
            if (!last) {
                ws.request(1);
                return CompletableFuture.completedStage(null);
            }

            final var body = stringBuf.toString();
            stringBuf.setLength(0);

            logger.trace("dashscope-client/exchange/handler/text >>> {}", body);
            return CompletableFuture.completedStage(body)
                    .thenApply(decoder)
                    .thenCompose(handler::onData)
                    .thenAccept(unused -> ws.request(1))
                    .exceptionallyCompose(this::fireClosed);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            logger.trace("dashscope-client/exchange/handler/binary bytes[{}]", data.capacity());
            return CompletableFuture.completedStage(data)
                    .thenCompose(handler::onBinary)
                    .thenAccept(unused -> ws.request(1))
                    .exceptionallyCompose(this::fireClosed);
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            return WebSocket.Listener.super.onPing(ws, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            return WebSocket.Listener.super.onPong(ws, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            logger.trace("dashscope-client/exchange/handler/close status={};reason={};", status, reason);
            final Throwable cause = (status == WebSocket.NORMAL_CLOSURE)
                    ? null
                    : new WebSocketCloseException(status, reason);
            return fireClosed(cause)
                    .exceptionally(ignored -> null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable ex) {
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
