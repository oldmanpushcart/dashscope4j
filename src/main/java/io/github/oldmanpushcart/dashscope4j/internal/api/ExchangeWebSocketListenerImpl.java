package io.github.oldmanpushcart.dashscope4j.internal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Exchange.NORMAL_CLOSURE;

@Slf4j
class ExchangeWebSocketListenerImpl<T extends ApiRequest<R>, R extends ApiResponse<?>> extends WebSocketListener {

    private final CompletableFuture<Exchange<T>> exchangeF;
    private final String uuid;
    private final Exchange.Mode mode;
    private final Exchange.Listener<T, R> listener;
    private final Function<T, String> encoder;
    private final BiFunction<Response, String, R> decoder;

    private final CompletableFuture<?> closeF = new CompletableFuture<>();
    private volatile okhttp3.Response httpResponse;

    ExchangeWebSocketListenerImpl(final CompletableFuture<Exchange<T>> exchangeF,
                                  final String uuid,
                                  final Exchange.Mode mode,
                                  final Exchange.Listener<T, R> listener,
                                  final Function<T, String> encoder,
                                  final BiFunction<okhttp3.Response, String, R> decoder) {
        this.exchangeF = exchangeF;
        this.uuid = uuid;
        this.mode = mode;
        this.listener = listener;
        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public void onOpen(@NotNull WebSocket socket, @NotNull Response httpResponse) {
        log.trace("WEBSOCKET://{} <<< OPEN;", uuid);
        final Exchange<T> exchange = new ExchangeImpl<>(uuid, mode, socket, encoder, closeF);
        if (!exchangeF.complete(exchange)) {
            socket.cancel();
            throw new IllegalStateException("Exchange already completed!");
        }
        this.httpResponse = new Response.Builder(httpResponse)
                .header("x-request-id", uuid)
                .build();
        listener.onOpen(exchange);
    }

    @Override
    public void onClosing(@NotNull WebSocket socket, int code, @NotNull String reason) {
        log.trace("WEBSOCKET://{} <<< CLOSING;code={};reason={};", uuid, code, reason);
        socket.close(code, reason);
    }

    @Override
    public void onClosed(@NotNull WebSocket socket, int code, @NotNull String reason) {
        log.trace("WEBSOCKET://{} <<< CLOSE;code={};reason={};", uuid, code, reason);
        if (NORMAL_CLOSURE == code) {
            listener.onCompleted();
            closeF.complete(null);
        } else {
            final Exception ex = new IOException(String.format("Internal closed! code=%s;reason=%s;",
                    code,
                    reason
            ));
            listener.onError(ex);
            closeF.completeExceptionally(ex);
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket socket, @NotNull Throwable t, @Nullable Response response) {
        log.error("WEBSOCKET://{} <<< FAILURE;", uuid, t);

        /*
         * 如果交换对象还没有创建，说明连接还没有建立成功。
         * 此时需要通知交换对象建立失败，告知外部连接失败。
         */
        if (!exchangeF.isDone()) {
            exchangeF.completeExceptionally(t);
        }

        listener.onError(t);
        closeF.completeExceptionally(t);
    }

    @Override
    public void onMessage(@NotNull WebSocket socket, @NotNull String text) {
        log.trace("WEBSOCKET://{} <<< TEXT;text={};", uuid, text);
        final Exchange<T> exchange = exchangeF.join();
        final InFrame frame = JacksonJsonUtils.toObject(text, InFrame.class);
        assert Objects.equals(uuid, frame.header().uuid());

        switch (frame.header().type()) {

            case STARTED: {
                log.debug("dashscope://exchange/{}/{} started! payload={};",
                        mode,
                        uuid,
                        frame.payload()
                );
                return;
            }

            /*
             * 接收到服务端发送任务失败数据帧，只能表明内部发生了部分的错误，但不能说明需要立即关闭整个连接
             * 记录失败信息并继续接收消息
             */
            case FAILED: {
                log.warn("dashscope://exchange/{}/{} running failed! code={};desc={};payload={};",
                        mode,
                        uuid,
                        frame.header().code(),
                        frame.header().desc(),
                        frame.payload()
                );
                return;
            }

            /*
             * 接收到服务端发送任务结束数据帧，说明本次连接的任务已经完成，
             * 可以优雅地关闭连接。
             */
            case FINISHED: {
                log.debug("dashscope://exchange/{}/{} finished! payload={};",
                        mode,
                        uuid,
                        frame.payload()
                );

                /*
                 * FINISHED 数据帧中有些场景会包含了最终的数据结果
                 */
                final R data = decoder.apply(httpResponse, frame.payload());
                listener.onData(data);
                exchange.closing(NORMAL_CLOSURE, "finished!");
                return;
            }

            /*
             * 接收到服务端发送的文本数据帧，
             * 转换为数据交换应答对象
             */
            case GENERATED: {
                log.debug("dashscope://exchange/{}/{} generated! payload={};",
                        mode,
                        uuid,
                        frame.payload()
                );
                final R data = decoder.apply(httpResponse, frame.payload());
                listener.onData(data);
            }

        }

    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        log.trace("WEBSOCKET://{} <<< BINARY;bytes={};", uuid, bytes.size());
        listener.onByteBuffer(bytes.asByteBuffer());
    }

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    private static class InFrame {

        @JsonProperty("header")
        Header header;

        @JsonDeserialize(using = ToStringDeserializer.class)
        @JsonProperty("payload")
        String payload;

        static class ToStringDeserializer extends JsonDeserializer<String> {
            @Override
            public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                return parser.getCodec().<JsonNode>readTree(parser).toString();
            }
        }

        @Value
        @Accessors(fluent = true)
        @Builder(access = AccessLevel.PRIVATE)
        @Jacksonized
        static class Header {

            @JsonProperty("task_id")
            String uuid;

            @JsonProperty("event")
            Type type;

            @JsonProperty("error_code")
            String code;

            @JsonProperty("error_message")
            String desc;

        }

        enum Type {

            @JsonProperty("task-started")
            STARTED,

            @JsonProperty("result-generated")
            GENERATED,

            @JsonProperty("task-failed")
            FAILED,

            @JsonProperty("task-finished")
            FINISHED

        }

    }

}
