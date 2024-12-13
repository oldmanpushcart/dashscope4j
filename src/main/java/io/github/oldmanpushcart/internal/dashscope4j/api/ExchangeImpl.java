package io.github.oldmanpushcart.internal.dashscope4j.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import okhttp3.WebSocket;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Accessors(fluent = true)
@AllArgsConstructor
class ExchangeImpl<T> implements Exchange<T> {

    @Getter
    private final String uuid;

    @Getter
    private final Exchange.Mode mode;

    private final WebSocket socket;
    private final Function<T, String> encoder;
    private final CompletableFuture<?> closeF;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean isFirstRef = new AtomicBoolean(true);

    /**
     * 校验当前帧是否为首帧
     *
     * @return TRUE | FALSE
     */
    private boolean isFirstFrame() {
        return isFirstRef.get()
               && isFirstRef.compareAndSet(true, false);
    }

    private boolean send(String text) {
        final boolean ret = socket.send(text);
        logger.trace("WEBSOCKET://{} >>> TEXT;ret={};text={};", uuid, ret, text);
        return ret;
    }

    @Override
    public boolean write(T data) {
        final OutFrame.Type type = isFirstFrame() ? OutFrame.Type.RUN : OutFrame.Type.CONTINUE;
        final OutFrame frame = new OutFrame(new OutFrame.Header(uuid, type, mode), encoder.apply(data));
        final String encoded = JacksonUtils.toJson(frame);
        return send(encoded);
    }

    @Override
    public boolean write(ByteBuffer buf) {
        final int remaining = buf.remaining();
        final ByteString byteString = ByteString.of(buf);
        final boolean ret = socket.send(byteString);
        logger.trace("WEBSOCKET://{} >>> BYTES;ret={};size={};", uuid, ret, remaining);
        return ret;
    }

    @Override
    public boolean finishing() {
        final OutFrame frame = new OutFrame(new OutFrame.Header(uuid, OutFrame.Type.FINISH, mode), "{\"input\": {}}");
        final String encoded = JacksonUtils.toJson(frame);
        return send(encoded);
    }

    @Override
    public boolean closing(int status, String reason) {
        final boolean ret = socket.close(status, reason);
        logger.trace("WEBSOCKET://{} >>> CLOSING;ret={};code={};reason={};", uuid, ret, status, reason);
        return ret;
    }

    @Override
    public void abort() {
        socket.cancel();
        logger.trace("WEBSOCKET://{} >>> ABORT;", uuid);
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
