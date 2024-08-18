package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;

public class ExchangeListenerAdapter<T, R> implements WebSocket.Listener {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
    private final String uuid;
    private final Exchange.Mode mode;
    private final Function<T, String> encoder;
    private final Function<String, R> decoder;
    private final Exchange.Listener<T, R> listener;

    private final CompletableFuture<Exchange<T, R>> exchangeF = new CompletableFuture<>();
    private final StringBuilder stringBuf = new StringBuilder();

    public ExchangeListenerAdapter(String uuid, Exchange.Mode mode, Function<T, String> encoder, Function<String, R> decoder, Exchange.Listener<T, R> listener) {
        this.uuid = uuid;
        this.mode = mode;
        this.encoder = encoder;
        this.decoder = decoder;
        this.listener = listener;
    }

    public CompletionStage<Exchange<T, R>> getExchange() {
        return exchangeF;
    }

    // 传播异常
    public <A> BiConsumer<A, Throwable> propagateEx(WebSocket socket) {
        return (o, ex) -> {
            if (null != ex) {
                onError(socket, ex);
            }
        };
    }

    @Override
    public void onOpen(WebSocket socket) {
        final var exchange = new ExchangeImpl<T, R>(socket, uuid, mode, encoder);
        if (!exchangeF.complete(exchange)) {
            socket.abort();
            throw new IllegalStateException("already bind!");
        }
        listener.onOpen(exchange);
        logger.debug("dashscope://exchange/{}/{} opened!", uuid, mode);
        logger.trace("WEBSOCKET: <<< OPEN");

    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {

        logger.trace("WEBSOCKET: <<< TEXT;last={};text={}", last, data);

        stringBuf.append(data);
        if (!last) {
            socket.request(1);
            return null;
        }

        final var text = stringBuf.toString();
        stringBuf.setLength(0);

        final var exchange = exchangeF.join();
        final var frame = JacksonUtils.toObject(text, OutFrame.class);
        final var header = frame.header();
        assert Objects.equals(uuid, header.uuid());

        switch (header.type()) {

            case STARTED -> {
                socket.request(1);
                return null;
            }

            /*
             * 接收到服务端发送任务失败数据帧，只能表明内部发生了部分的错误，但不能说明需要立即关闭整个连接
             * 记录失败信息并继续接收消息
             */
            case FAILED -> {
                logger.warn("dashscope://exchange/{}/{} running failed! code={};message={}; << {}",
                        uuid,
                        mode,
                        header.code(),
                        header.message(),
                        text
                );
                socket.request(1);
                return null;
            }

            /*
             * 接收到服务端发送任务结束数据帧，说明本次连接的任务已经完成，可以优雅地关闭连接
             * 任务结束数据帧呆着最后一个可被序列化的信息
             */
            case FINISHED -> {
                logger.debug("dashscope://exchange/{}/{} finished!", uuid, mode);
                return listener.onData(exchange, decoder.apply(frame.payload()))
                        .thenCompose(v -> exchange.close(1000, "finished"))
                        .whenComplete(propagateEx(socket));
            }

            /*
             * 接收到服务端发送的文本数据帧，
             * 转换为数据交换应答对象
             */
            case GENERATED -> {
                return listener.onData(exchange, decoder.apply(frame.payload()))
                        .whenComplete(propagateEx(socket));
            }

            /*
             * 其他情况不应该存在，直接抛出异常
             */
            default -> throw new UnsupportedOperationException("Unsupported Type: %s".formatted(
                    header.type()
            ));
        }

    }

    @Override
    public CompletionStage<?> onBinary(WebSocket socket, ByteBuffer buf, boolean last) {
        logger.trace("WEBSOCKET: <<< BINARY;last={};bytes={}", last, buf.remaining());
        return listener.onByteBuffer(exchangeF.join(), buf, last)
                .whenComplete(propagateEx(socket));
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int status, String reason) {
        logger.trace("WEBSOCKET: <<< CLOSE;status={};reason={};", status, reason);
        return listener.onCompleted(exchangeF.join(), status, reason)
                .whenComplete(propagateEx(socket));
    }

    @Override
    public void onError(WebSocket socket, Throwable ex) {
        logger.trace("WEBSOCKET: <<< ERROR;", ex);
        listener.onError(exchangeF.join(), ex);
    }

}
