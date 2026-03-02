package io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils;
import okhttp3.*;
import okio.ByteString;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DefaultRealtimeApi implements RealtimeApi, InternalContents {

    /**
     * 正常关闭
     */
    private static final int NORMAL_CLOSURE = 1000;

    /**
     * 内部错误
     * <p>遇到未预期的状态或错误</p>
     */
    private static final int INTERNAL_ERROR_CLOSURE = 1011;

    private final String host;
    private final String ak;
    private final OkHttpClient http;

    public DefaultRealtimeApi(String host, String ak, OkHttpClient http) {
        this.host = host;
        this.ak = ak;
        this.http = http;
    }

    @Override
    public <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler) {

        final var id = UUIDUtils.genUUID22();
        final var stringHandler = session.provider().apply(handler);
        final var futureListener = new FutureListener(id, stringHandler);

        final var endpoint = EndpointUtils.wss(host, session.model().path());
        final var httpRequest = new Request.Builder()
                .url(endpoint.toString())
                .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                .build();
        http.newWebSocket(httpRequest, futureListener);
        return futureListener.getFuture();
    }

    private static class StringEmitter implements Realtime.Emitter<String> {

        private final String uuid;
        private final WebSocket ws;
        private final CompletableFuture<Void> closeF;

        private StringEmitter(String uuid, WebSocket ws, CompletableFuture<Void> closeF) {
            this.uuid = uuid;
            this.ws = ws;
            this.closeF = closeF;
        }

        private void sending(Supplier<Boolean> action) {
            if (isClosed()) {
                throw new IllegalStateException("Already closed!");
            }
            if (!action.get()) {
                throw new IllegalStateException("Send failed!");
            }
        }

        @Override
        public Realtime.Emitter<String> data(String in) {
            sending(() -> ws.send(in));
            return this;
        }

        @Override
        public Realtime.Emitter<String> binary(ByteBuffer buffer) {
            sending(() -> {
                final ByteString byteString = ByteString.of(buffer);
                return ws.send(byteString);
            });
            return this;
        }

        @Override
        public void close() {
            try {
                sending(() -> ws.close(NORMAL_CLOSURE, "Bye!"));
            } catch (Throwable t) {
                ws.cancel();
            }
        }

        @Override
        public void close(Throwable ex) {
            try {
                sending(() -> ws.close(INTERNAL_ERROR_CLOSURE, "Internal error"));
            } catch (Throwable t) {
                ws.cancel();
            }
        }

        @Override
        public String id() {
            return uuid;
        }

        @Override
        public boolean isClosed() {
            return closeF.isDone();
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return closeF;
        }

    }

    private static class FutureListener extends WebSocketListener {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final String uuid;
        private final Realtime.Handler<String, String> handler;
        private final CompletableFuture<Realtime.Emitter<String>> completeF = new CompletableFuture<>();
        private final CompletableFuture<Void> closeF = new CompletableFuture<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final String _toString;

        private FutureListener(String uuid, Realtime.Handler<String, String> handler) {
            this.uuid = uuid;
            this.handler = handler;
            this._toString = "dashscope4j-client://realtime/%s".formatted(uuid);
        }

        @Override
        public String toString() {
            return _toString;
        }

        public CompletionStage<Realtime.Emitter<String>> getFuture() {
            return completeF;
        }

        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            try {
                final var emitter = new StringEmitter(uuid, ws, closeF);
                handler.onOpen(emitter);
                completeF.complete(emitter);
                logger.debug("{} opened.", this);
            } catch (Throwable ex) {

                logger.warn("{} open failure.", this, ex);

                // 立即取消连接，释放宝贵地连接资源
                ws.cancel();

                // 标记关闭（防止后续因为 ws.cancel 导致 onClosed 被触发而重复执行
                if (closed.compareAndSet(false, true)) {
                    fireHandler(() -> handler.onClosed(ex));
                    completeF.completeExceptionally(ex);
                    closeF.completeExceptionally(ex);
                }

                /*
                 * 如果出现了竞态： onClosed（由 ws.cancel 引起） 先于 onOpen 的异常处理而执行，
                 * 这里也必须要将 completeF 完成，确保拿到 completeF 流程能继续。
                 */
                else {
                    completeF.completeExceptionally(ex);
                }

            }
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            handler.onData(text);
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull ByteString bytes) {
            handler.onBinary(bytes.asByteBuffer());
        }

        private void fireHandler(Runnable action) {
            try {
                action.run();
            } catch (Throwable ex) {
                logger.warn("{} handler error", this, ex);
            }
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            switch (code) {
                case 1000, 1001 -> {
                    logger.debug("{} closed normally.", this);
                    fireHandler(() -> handler.onClosed(null));
                    closeF.complete(null);
                }
                default -> {
                    final var ioEx = new IOException("Closed abnormally, code:%s;reason:%s".formatted(
                            code,
                            reason
                    ));
                    logger.warn("{} closed abnormally.", this, ioEx);
                    fireHandler(() -> handler.onClosed(ioEx));
                    closeF.completeExceptionally(ioEx);
                }
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response httpResponse) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            logger.warn("{} closed abnormally!", this, t);
            fireHandler(() -> handler.onClosed(t));
            closeF.completeExceptionally(t);
        }

    }

}
