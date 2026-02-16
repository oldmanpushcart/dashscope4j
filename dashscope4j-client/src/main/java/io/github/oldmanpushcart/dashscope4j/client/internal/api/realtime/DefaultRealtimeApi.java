package io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.Config;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
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

import static io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents.*;

public class DefaultRealtimeApi implements RealtimeApi {

    private final Config config;
    private final HttpClient http;

    public DefaultRealtimeApi(Config config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler) {
        final var id = UUIDUtils.genUUID22();
        final var endpoint = EndpointUtils.wss(config.host(), session.model().path());
        final var stringHandler = session.provider().apply(handler);
        final var listener = new ListenerImpl(id, endpoint, stringHandler);
        final var builder = http.newWebSocketBuilder()
                .header(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .header(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(config.ak()))
                .header(HTTP_HEADER_X_DASHSCOPE_SSE, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                .header(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE);

        if (config.httpConnectTimeout() != null) {
            builder.connectTimeout(config.httpConnectTimeout());
        }

        //noinspection resource
        final var scope = Tracer.instance.enter("websocket");
        scope.span()
                .property("uri", String.valueOf(endpoint));
        return builder
                .buildAsync(endpoint, listener)
                .whenComplete((r, ex) -> {
                    final var span = scope.restore();
                    if (null == ex) {
                        span.success()
                                .property("sub-protocol", r.getSubprotocol());
                    } else {
                        span.failure(ex);
                    }
                    scope.close();
                })
                .thenCompose(ws -> listener.getFuture());
    }

    private static class Emitter implements Realtime.Emitter<String> {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final WebSocket ws;
        private final CompletableFuture<Void> closeF;
        private final ListenerImpl listener;
        private final String _toString;

        private Emitter(String id, WebSocket ws, CompletableFuture<Void> closeF, ListenerImpl listener) {
            this.id = id;
            this.ws = ws;
            this.closeF = closeF;
            this.listener = listener;
            this._toString = "dashscope4j-client://realtime/%s".formatted(id);
        }

        @Override
        public String toString() {
            return _toString;
        }

        @Override
        public CompletionStage<Void> data(String input) {
            return ws.sendText(input, true)
                    .whenComplete((v, ex) -> {
                        if (null == ex) {
                            logger.debug("{}/text >>> {}", this, input);
                        } else {
                            logger.warn("{}/text >>> {}", this, input, ex);
                        }
                    })
                    .thenAccept(unused -> {
                    });
        }

        @Override
        public CompletionStage<Void> binary(ByteBuffer buffer) {
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

        @Override
        public CompletionStage<Void> closing() {
            return requestClose(WebSocket.NORMAL_CLOSURE, "bye!");
        }

        @Override
        public CompletionStage<Void> closing(Throwable ex) {
            return requestClose(1008, "Inner Error!");
        }

        private CompletionStage<Void> requestClose(int code, String reason) {
            return ws.sendClose(code, reason)
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
        public String id() {
            return id;
        }

        @Override
        public boolean isClosed() {
            return closeF.isDone();
        }

        @Override
        public void close() {
            ws.abort();
            listener.fireClosed(ws, null);
            logger.debug("{} >>> CLOSED", this);
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return closeF;
        }
    }

    private static class ListenerImpl implements WebSocket.Listener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String id;
        private final URI endpoint;
        private final Realtime.Handler<String, String> handler;

        private final CompletableFuture<Realtime.Emitter<String>> completeF = new CompletableFuture<>();
        private final CompletableFuture<Void> closeF = new CompletableFuture<>();
        private final StringBuilder stringBuf = new StringBuilder();

        private final String _toString;

        private ListenerImpl(String id, URI endpoint, Realtime.Handler<String, String> handler) {
            this.id = id;
            this.endpoint = endpoint;
            this.handler = handler;
            this._toString = "dashscope4j-client://realtime/%s".formatted(id);
        }

        public CompletionStage<Realtime.Emitter<String>> getFuture() {
            return completeF;
        }

        public String toString() {
            return _toString;
        }

        @Override
        public void onOpen(WebSocket ws) {
            try {
                final var emitter = new Emitter(id, ws, closeF, this);
                handler.onOpen(emitter);
                completeF.complete(emitter);
                ws.request(1L);
                logger.debug("{} opened. endpoint={};", this, endpoint);
            } catch (Throwable ex) {
                fireClosed(ws, ex);
                logger.warn("{} open failure. endpoint={};", this, endpoint, ex);
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
        void fireClosed(WebSocket ws, Throwable ex) {

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
                    ? new IllegalStateException("websocket closed with status %d: %s".formatted(status, reason))
                    : null;
            fireClosed(ws, ex);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable ex) {
            fireClosed(ws, ex);
        }

    }

}
