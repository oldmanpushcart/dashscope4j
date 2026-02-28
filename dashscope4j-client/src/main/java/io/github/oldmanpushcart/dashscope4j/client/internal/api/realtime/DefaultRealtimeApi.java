package io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class DefaultRealtimeApi implements RealtimeApi, InternalContents {

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
        final var endpoint = EndpointUtils.wss(host, session.model().path());
        final var stringHandler = session.provider().apply(handler);

        return null;
    }

    private static class StringEmitter implements Realtime.Emitter<String> {

        private static final CompletionStage<Void> SUCCESS_F = CompletableFuture.completedFuture(null);

        /**
         * 正常关闭
         */
        private static final int NORMAL_CLOSURE = 1000;

        /**
         * 内部错误
         * <p>遇到未预期的状态或错误</p>
         */
        private static final int INTERNAL_ERROR_CLOSURE = 1011;

        private final String uuid;
        private final WebSocket ws;
        private final CompletableFuture<Void> closeF;

        private StringEmitter(String uuid, WebSocket ws, CompletableFuture<Void> closeF) {
            this.uuid = uuid;
            this.ws = ws;
            this.closeF = closeF;
        }

        private CompletionStage<Void> sending(Supplier<Boolean> action) {
            try {
                if (!action.get()) {
                    throw new IllegalStateException("WebSocket send failed!");
                }
                return SUCCESS_F;
            } catch (Throwable ex) {
                return CompletableFuture.failedFuture(ex);
            }
        }

        @Override
        public CompletionStage<Void> data(String in) {
            return sending(() -> ws.send(in));
        }

        @Override
        public CompletionStage<Void> binary(ByteBuffer buffer) {
            return sending(() -> {
                final ByteString byteString = ByteString.of(buffer);
                return ws.send(byteString);
            });
        }

        @Override
        public CompletionStage<Void> closing() {
            return sending(() -> ws.close(NORMAL_CLOSURE, "Bye!"));
        }

        @Override
        public CompletionStage<Void> closing(Throwable ex) {
            return sending(() -> ws.close(INTERNAL_ERROR_CLOSURE, "Internal error"));
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
        public void close() {
            ws.cancel();
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
        private final StringBuilder stringBuf = new StringBuilder();

        private FutureListener(String uuid, Realtime.Handler<String, String> handler) {
            this.uuid = uuid;
            this.handler = handler;
        }

        public CompletionStage<Realtime.Emitter<String>> getFuture() {
            return completeF;
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
                ws.cancel();
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
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
            super.onMessage(webSocket, bytes);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
        }

    }

}
