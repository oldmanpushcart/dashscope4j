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
        final var id = UUID.randomUUID().toString();
        return http.newWebSocketBuilder()
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                .buildAsync(endpoint, new ListenerImpl<>(id, endpoint, encoder, decoder, handler))
                .thenApply(ws -> new ExchangeImpl<>(id, ws, encoder));
    }

    private static class ExchangeImpl<T> implements Exchange<T> {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final WebSocket ws;
        private final Function<T, String> encoder;

        private ExchangeImpl(String id, WebSocket ws, Function<T, String> encoder) {
            this.id = id;
            this.ws = ws;
            this.encoder = encoder;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isClosed() {
            return ws.isInputClosed() || ws.isOutputClosed();
        }

        @Override
        public CompletionStage<Void> closing() {
            logger.trace("dashscope-client://exchange/{} >>> CLOSING.", id);
            return ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye!")
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public void close() {
            logger.trace("dashscope-client://exchange/{} >>> CLOSED.", id);
            ws.abort();
        }

        @Override
        public CompletionStage<Void> send(T data) {
            final var body = encoder.apply(data);
            logger.trace("dashscope-client://exchange/{}/text >>> {}", id, body);
            return ws.sendText(body, true)
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            logger.trace("dashscope-client://exchange/{}/binary >>> bytes[{}]", id, buffer.remaining());
            return ws.sendBinary(buffer, true)
                    .thenAccept(unused -> {
                    });
        }

    }


    /*
     * Exchange的WebSocket监听器
     * 用于驱动Exchange处理器
     */
    private static class ListenerImpl<T, R, E> implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final URI endpoint;
        private final Function<T, String> encoder;
        private final Function<String, R> decoder;
        private final Exchange.Handler<T, R> handler;

        private final StringBuilder stringBuf = new StringBuilder();
        private final AtomicBoolean closedFlag = new AtomicBoolean(false);

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
        }

        @Override
        public void onOpen(WebSocket ws) {
            try {
                final var exchange = new ExchangeImpl<>(id, ws, encoder);
                handler.onOpen(exchange);
                ws.request(1L);
                logger.trace("dashscope-client://exchange/{} opened. endpoint={};", id, endpoint);
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
             * 都已经通知关闭了，所以这里无论如何也得中断一次webscoket
             * 反正是幂等操作，关了心安
             */
            ws.abort();

            logger.trace("dashscope-client://exchange/{} closed.", id, ex);
            return CompletableFuture.completedStage(null)
                    .thenAccept(unused -> handler.onClosed(ex))
                    .exceptionally(closeEx -> {
                        logger.trace("dashscope-client://exchange/{} occur error when fire closed!", id, closeEx);
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

            logger.trace("dashscope-client://exchange/{}/text <<< {}", id, body);
            return CompletableFuture.completedStage(body)
                    .thenApply(decoder)
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
            logger.trace("dashscope-client://exchange/{}/binary <<< bytes[{}]", id, data.remaining());
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
            logger.trace("dashscope-client://exchange/{}/ping <<< bytes[{}]", id, message.remaining());
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
            logger.trace("dashscope-client://exchange/{}/pong <<< bytes[{}]", id, message.remaining());
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
