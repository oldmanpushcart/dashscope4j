package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class ExchangeApiExecutor {

    private final String ak;
    private final HttpClient http;

    public ExchangeApiExecutor(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    public <T, R> CompletionStage<Exchange<T>> newExchange(
            final URI endpoint,
            final Function<T, String> encoder,
            final Function<String, R> decoder,
            final Exchange.Handler<T, R> handler
    ) {
        final var id = UUIDUtils.genUUID22();
        final var listener = new ListenerImpl<>(id, endpoint, encoder, decoder, handler);
        return http.newWebSocketBuilder()
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .buildAsync(endpoint, listener)
                .thenCompose(ws -> listener.getFuture());
    }

    private static class ExchangeImpl<T> implements Exchange<T> {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final WebSocket ws;
        private final Function<T, String> encoder;
        private final CompletableFuture<Void> closeF;
        private final String _toString;

        private ExchangeImpl(String id, WebSocket ws, Function<T, String> encoder, CompletableFuture<Void> closeF) {
            this.id = id;
            this.ws = ws;
            this.encoder = encoder;
            this.closeF = closeF;
            this._toString = "dashscope4j-client://exchange/%s".formatted(id);
        }

        @Override
        public String toString() {
            return _toString;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isClosed() {
            return closeF.isDone();
        }

        @Override
        public CompletionStage<Void> closing() {
            return ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye!")
                    .whenComplete((v, ex) -> {
                        if (null == ex) {
                            logger.debug("{} >>> CLOSING", this);
                        } else {
                            logger.warn("{} >>> CLOSING", this, ex);
                        }
                    })
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public void close() {
            ws.abort();
            logger.debug("{} >>> CLOSED", this);
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return closeF;
        }

        @Override
        public CompletionStage<Void> send(T data) {
            final var body = encoder.apply(data);
            return ws.sendText(body, true)
                    .whenComplete((v, ex) -> {
                        if (null == ex) {
                            logger.debug("{}/text >>> {}", this, body);
                        } else {
                            logger.warn("{}/text >>> {}", this, body, ex);
                        }
                    })
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            final var byteCnt = buffer.remaining();
            return ws.sendBinary(buffer, true)
                    .whenComplete((v, ex) -> {
                        if (null == ex) {
                            logger.debug("{}/binary >>> bytes[{}]", this, byteCnt);
                        } else {
                            logger.warn("{}/binary >>> bytes[{}]", this, byteCnt, ex);
                        }
                    })
                    .thenAccept(unused -> {
                    });
        }

    }


    /*
     * Exchange的WebSocket监听器
     * 用于驱动Exchange处理器
     */
    private static class ListenerImpl<T, R> implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final URI endpoint;
        private final Function<T, String> encoder;
        private final Function<String, R> decoder;
        private final Exchange.Handler<T, R> handler;

        private final CompletableFuture<Exchange<T>> exchangeF = new CompletableFuture<>();
        private final CompletableFuture<Void> closeF = new CompletableFuture<>();
        private final StringBuilder stringBuf = new StringBuilder();

        private final String _toString;

        private ListenerImpl(
                final String id,
                final URI endpoint,
                final Function<T, String> encoder,
                final Function<String, R> decoder,
                final Exchange.Handler<T, R> handler
        ) {
            this.id = id;
            this.endpoint = endpoint;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
            this._toString = "dashscope4j-client://exchange/%s".formatted(id);
        }

        public CompletionStage<Exchange<T>> getFuture() {
            return exchangeF;
        }

        @Override
        public String toString() {
            return _toString;
        }

        @Override
        public void onOpen(WebSocket ws) {
            try {
                final var exchange = new ExchangeImpl<>(id, ws, encoder, closeF);
                handler.onOpen(exchange);
                exchangeF.complete(exchange);
                ws.request(1L);
                logger.debug("{} opened. endpoint={};", this, endpoint);
            } catch (Throwable ex) {
                fireClosed(ws, ex);
                logger.warn("{} open failure. endpoint={};", this, endpoint, ex);
            }
        }

        private boolean tryClose(Throwable ex) {
            if (null == ex) {
                return closeF.complete(null);
            } else {
                return closeF.completeExceptionally(ex);
            }
        }

        /**
         * 触发关闭处理
         * <p>
         * 关闭处理只有第一次触发有效
         * </p>
         *
         * @param ws websocket
         * @param ex 错误信息
         */
        private void fireClosed(WebSocket ws, Throwable ex) {

            /*
             * 错误标志位，防止重复触发
             *
             * 在进行关闭处理过程中，很可能会触发二次关闭（比如websocket.about())，
             * 所以这里进行一次重复调用判断，只有第一次才触发
             */
            if (!tryClose(ex)) {
                return;
            }

            /*
             * 都已经通知关闭了，所以这里无论如何也得中断一次连接
             * 反正是幂等操作，关了心安
             */
            try {
                ws.abort();
            } catch (Throwable abortEx) {
                logger.warn("{} websocket abort threw exception during close", this, abortEx);
            }

            /*
             * 努力通知 handler 当前通道已关闭
             * 通知失败不影响整个关闭流程
             */
            try {
                handler.onClosed(ex);
            } catch (Throwable closeEx) {
                logger.warn("{} handler threw exception during close", this, closeEx);
            }

            if (ex == null) {
                logger.debug("{} closed normally", this);
            } else {
                logger.warn("{} closed abnormally", this, ex);
            }

        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence string, boolean last) {

            /*
             * 滚动添加文本
             * 直last==true时，将滚动添加的文本转为字符串，并清空滚动添加的文本缓存
             */
            stringBuf.append(string);
            if (!last) {
                ws.request(1);
                return null;
            }
            final var body = stringBuf.toString();
            stringBuf.setLength(0);

            logger.debug("{} <<< {}", this, body);
            return CompletableFuture.completedStage(body)
                    .thenApply(decoder)
                    .thenCompose(handler::onData)
                    .whenComplete((unused, ex) -> {
                        if (null == ex) {
                            ws.request(1);
                        } else {
                            fireClosed(ws, ex);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            final var byteCnt = data.remaining();
            logger.debug("{} <<< bytes[{}]!", this, byteCnt);
            return CompletableFuture.completedStage(data)
                    .thenCompose(handler::onBinary)
                    .whenComplete((unused, ex) -> {
                        if (null == ex) {
                            ws.request(1);
                        } else {
                            fireClosed(ws, ex);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            final var byteCnt = message.remaining();
            logger.debug("{} <<< PING({}bytes)", this, byteCnt);
            return ws.sendPong(message)
                    .whenComplete((unused, ex) -> {
                        if (null == ex) {
                            ws.request(1);
                        } else {
                            fireClosed(ws, ex);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            final var byteCnt = message.remaining();
            logger.debug("{} <<< PONG({}bytes)", this, byteCnt);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            logger.trace("{} <<< CLOSE! status={};reason={};", this, status, reason);
            final var ex = status != WebSocket.NORMAL_CLOSURE
                    ? new WebSocketCloseException(status, reason)
                    : null;
            fireClosed(ws, ex);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable ex) {
            fireClosed(ws, ex);
        }

    }

    /**
     * WebSocket 关闭异常
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
