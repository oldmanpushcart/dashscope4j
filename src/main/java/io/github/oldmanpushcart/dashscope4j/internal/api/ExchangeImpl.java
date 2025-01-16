package io.github.oldmanpushcart.dashscope4j.internal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.WEBSOCKET_CLOSE_REASON_MAX_LENGTH;
import static io.github.oldmanpushcart.dashscope4j.internal.util.StringUtils.substring;

@Accessors(fluent = true)
@Slf4j
class ExchangeImpl<T> implements Exchange<T> {

    @Getter
    private final String uuid;

    @Getter
    private final Exchange.Mode mode;

    private final WebSocket socket;
    private final Function<T, String> encoder;
    private final CompletableFuture<?> closeF;

    private final AtomicBoolean isFirstRef = new AtomicBoolean(true);
    private final CompositeDisposable disposes = new CompositeDisposable();

    ExchangeImpl(final String uuid,
                 final Mode mode,
                 final WebSocket socket,
                 final Function<T, String> encoder,
                 final CompletableFuture<?> closeF) {
        this.uuid = uuid;
        this.mode = mode;
        this.socket = socket;
        this.encoder = encoder;
        this.closeF = closeF
                .whenComplete((v, ex) -> disposes.dispose());
    }

    /**
     * 校验当前帧是否为首帧
     *
     * @return TRUE | FALSE
     */
    private boolean isFirstFrame() {
        return isFirstRef.get()
               && isFirstRef.compareAndSet(true, false);
    }

    @Override
    public boolean write(T data) {
        return writeData(data);
    }

    @Override
    public boolean writeData(T data) {
        final OutFrame.Type type = isFirstFrame() ? OutFrame.Type.RUN : OutFrame.Type.CONTINUE;
        final OutFrame frame = new OutFrame(new OutFrame.Header(uuid, type, mode), encoder.apply(data));
        final String encoded = JacksonJsonUtils.toJson(frame);
        final boolean ret = socket.send(encoded);
        log.trace("WEBSOCKET://{} >>> DATA;ret={};payload={};", uuid, ret, encoded);
        return ret;
    }

    @Override
    public boolean write(ByteBuffer buf) {
        return writeByteBuffer(buf);
    }

    @Override
    public boolean writeByteBuffer(ByteBuffer buf) {
        final int remaining = buf.remaining();
        final ByteString byteString = ByteString.of(buf);
        final boolean ret = socket.send(byteString);
        log.trace("WEBSOCKET://{} >>> BYTES;ret={};size={};", uuid, ret, remaining);
        return ret;
    }

    @Override
    public Disposable subscribeForWriteData(Flowable<T> flow, boolean finishingAfterWrite) {
        final Disposable dispose;
        dispose = flow.subscribe(
                data -> {
                    if (!writeData(data)) {
                        abort();
                    }
                },
                ex -> {
                    if (!closing(ex)) {
                        abort();
                    }
                },
                () -> {
                    disposes.remove(disposes);
                    if (finishingAfterWrite) {
                        if (!finishing()) {
                            abort();
                        }
                    }
                });
        disposes.add(dispose);
        return dispose;
    }

    @Override
    public Disposable subscribeForWriteByteBuffer(Flowable<ByteBuffer> flow, boolean finishingAfterWrite) {
        final Disposable dispose;
        dispose = flow.subscribe(
                buf -> {
                    if (!writeByteBuffer(buf)) {
                        abort();
                    }
                },
                ex -> {
                    if (!closing(ex)) {
                        abort();
                    }
                },
                () -> {
                    disposes.remove(disposes);
                    if (finishingAfterWrite) {
                        if (!finishing()) {
                            abort();
                        }
                    }
                });
        disposes.add(dispose);
        return dispose;
    }

    @Override
    public boolean finishing() {
        final OutFrame frame = new OutFrame(new OutFrame.Header(uuid, OutFrame.Type.FINISH, mode), "{\"input\": {}}");
        final String encoded = JacksonJsonUtils.toJson(frame);
        final boolean ret = socket.send(encoded);
        log.trace("WEBSOCKET://{} >>> FINISHING;ret={};payload={};", uuid, ret, encoded);
        return ret;
    }

    @Override
    public boolean closing(int status, String reason) {
        final boolean ret = socket.close(status, substring(reason, WEBSOCKET_CLOSE_REASON_MAX_LENGTH));
        log.trace("WEBSOCKET://{} >>> CLOSING;ret={};code={};reason={};", uuid, ret, status, reason);
        return ret;
    }

    @Override
    public boolean closing(Throwable ex) {
        if (null == ex) {
            return closing();
        }
        final String reason = ex.getClass().getSimpleName();
        final boolean ret = socket.close(INTERNAL_ERROR_CLOSURE, substring(reason, WEBSOCKET_CLOSE_REASON_MAX_LENGTH));
        log.trace("WEBSOCKET://{} >>> CLOSING;ret={};code={};reason={};", uuid, ret, INTERNAL_ERROR_CLOSURE, reason, ex);
        return ret;
    }

    @Override
    public boolean closing() {
        return closing(NORMAL_CLOSURE, "close by normal");
    }

    @Override
    public void abort() {
        socket.cancel();
        log.trace("WEBSOCKET://{} >>> ABORT;", uuid);
    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public CompletionStage<?> closeStage() {
        return closeF;
    }


    @Value
    @Accessors(fluent = true)
    private static class OutFrame {

        @JsonProperty("header")
        Header header;

        @JsonProperty("payload")
        @JsonRawValue
        String payload;

        @Value
        @Accessors(fluent = true)
        public static class Header {

            @JsonProperty("task_id")
            String uuid;

            @JsonProperty("action")
            Type type;

            @JsonProperty("streaming")
            Exchange.Mode mode;

        }

        public enum Type {

            @JsonProperty("run-task")
            RUN,

            @JsonProperty("continue-task")
            CONTINUE,

            @JsonProperty("finish-task")
            FINISH

        }

    }

}
