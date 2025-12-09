package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class ExchangeApiExecutor {

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
        final var uuid = UUID.randomUUID().toString();
        final var exchangeFutureListener = new ExchangeFutureListener<>(uuid, endpoint, codec, handler);
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

        private final String uuid;
        private final WebSocket ws;
        private final Exchange.Codec<T, R> codec;

        private ExchangeImpl(String uuid, WebSocket ws, Codec<T, R> codec) {
            this.uuid = uuid;
            this.ws = ws;
            this.codec = codec;
        }

        @Override
        public String uuid() {
            return uuid;
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

    /*
     * Exchange的WebSocket监听器
     * 用于驱动Exchange处理器
     */
    private static class ExchangeFutureListener<T, R> implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String uuid;
        private final URI endpoint;
        private final Exchange.Codec<T, R> codec;
        private final Exchange.Handler<T, R> handler;

        private final CompletableFuture<Exchange<T, R>> exchangeF = new CompletableFuture<>();
        private final StringBuilder stringBuf = new StringBuilder();
        private final AtomicBoolean closedFlag = new AtomicBoolean(false);

        private ExchangeFutureListener(
                final String uuid,
                final URI endpoint,
                final Exchange.Codec<T, R> codec,
                final Exchange.Handler<T, R> handler
        ) {
            this.uuid = uuid;
            this.endpoint = endpoint;
            this.codec = codec;
            this.handler = handler;
        }

        public CompletionStage<Exchange<T, R>> getFuture() {
            return exchangeF;
        }

        @Override
        public void onOpen(WebSocket ws) {
            logger.trace("dashscope-client://exchange/{} opened. endpoint={};", uuid, endpoint);
            try {
                final var exchange = new ExchangeImpl<>(uuid, ws, codec);
                handler.onOpen(exchange);
                exchangeF.complete(exchange);
                ws.request(1);
            } catch (Throwable ex) {
                fireClosed(ws, ex);
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
         * @return 关闭处理结果
         */
        private CompletionStage<Void> fireClosed(WebSocket ws, Throwable ex) {

            /*
             * 错误标志位，防止重复触发
             *
             * 在进行关闭处理过程中，很可能会触发二次关闭（比如websocket.about())，
             * 所以这里进行一次重复调用判断，只有第一次才触发
             */
            if (!closedFlag.compareAndSet(false, true)) {
                return CompletableFuture.completedStage(null);
            }

            /*
             * 如果是错误关闭，则需要中断当前的websocket
             * 中断websocket会触发二次关闭处理，所以前边必须要有一次重复触发判断
             */
            if (null != ex) {
                ws.abort();
            }

            logger.trace("dashscope-client://exchange/{} closed.", uuid, ex);
            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> handler.onClosed(ex))
                    .exceptionally(closeEx -> {
                        logger.trace("dashscope-client://exchange/{} occur error when fire closed!", uuid, closeEx);
                        return null;
                    });
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

            logger.trace("dashscope-client://exchange/{}/text <<< {}", uuid, body);
            return CompletableFuture.completedStage(body)
                    .thenApply(codec::decode)
                    .thenCompose(handler::onData)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            fireClosed(ws, ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
            logger.trace("dashscope-client://exchange/{}/binary <<< bytes[{}]", uuid, data.remaining());
            return CompletableFuture.completedStage(data)
                    .thenCompose(handler::onBinary)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            fireClosed(ws, ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            logger.trace("dashscope-client://exchange/{}/ping <<< bytes[{}]", uuid, message.remaining());
            return ws.sendPong(message)
                    .whenComplete((unused, ex) -> {
                        if (null != ex) {
                            fireClosed(ws, ex);
                        } else {
                            ws.request(1);
                        }
                    });
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            logger.trace("dashscope-client://exchange/{}/pong <<< bytes[{}]", uuid, message.remaining());
            return WebSocket.Listener.super.onPong(ws, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
            final var ex = status != WebSocket.NORMAL_CLOSURE
                    ? new WebSocketCloseException(status, reason)
                    : null;
            return fireClosed(ws, ex);
        }

        @Override
        public void onError(WebSocket ws, Throwable ex) {
            fireClosed(ws, ex);
        }

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
