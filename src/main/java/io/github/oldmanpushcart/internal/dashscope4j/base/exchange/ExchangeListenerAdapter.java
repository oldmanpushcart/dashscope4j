package io.github.oldmanpushcart.internal.dashscope4j.base.exchange;

import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.exchange.ExchangeException;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
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
    private final Executor executor;

    private final CompletableFuture<?> closeF = new CompletableFuture<>();
    private final CompletableFuture<Exchange<T, R>> exchangeF = new CompletableFuture<>();
    private final StringBuilder stringBuf = new StringBuilder();

    public ExchangeListenerAdapter(Executor executor, String uuid, Exchange.Mode mode, Function<T, String> encoder, Function<String, R> decoder, Exchange.Listener<T, R> listener) {
        this.executor = executor;
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
        final var exchange = new ExchangeImpl<T, R>(executor, socket, uuid, mode, closeF, encoder);
        if (!exchangeF.complete(exchange)) {
            socket.abort();
            throw new IllegalStateException("already bind!");
        }

        logger.trace("WEBSOCKET: <<< OPEN");

        try {
            listener.onOpen(exchange);
        } catch (Throwable t) {
            logger.warn("dashscope://exchange/{}/{} fire open occur error!", mode, uuid, t);
        }

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
                logger.debug("dashscope://exchange/{}/{} started!", mode, uuid);
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
                logger.debug("dashscope://exchange/{}/{} finished!", mode, uuid);
                return listener.onData(exchange, decoder.apply(frame.payload()))
                        .thenCompose(v -> exchange.closing(1000, "finished"))
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
            default -> throw new ExchangeException(uuid, mode, "Unsupported Type: %s".formatted(
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

    /**
     * 是否正常关闭
     *
     * @param status 关闭状态码
     * @return TRUE | FALSE
     */
    private static boolean isNormalClosure(int status) {

        /*
         * [1000-2999]为标准状态码
         * 当前判断策略在标准状态码上采用白名单策略
         */
        if (status >= 1000 && status <= 2999) {
            return switch (status) {
                case 1000, // normal closure
                     1001, // going away
                     1005, // no status received
                     1006  // abnormal closure
                        -> true;
                default -> false;
            };
        }

        /*
         * 其他状态范围判断策略采用黑名单策略，
         * 默认为正常状态
         */
        return true;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int status, String reason) {
        logger.trace("WEBSOCKET: <<< CLOSE;status={};reason={};", status, reason);

        /*
         * 检查是否正常关闭，如果不是则抛出异常并通知
         */
        if (!isNormalClosure(status)) {
            onError(socket, new ExchangeException.AbnormalClosedException(uuid, mode, status, reason));
            return null;
        }

        closeF.complete(null);
        try {
            return listener.onCompleted(exchangeF.join(), status, reason);
        } catch (Throwable t) {
            logger.warn("dashscope://exchange/{}/{} fire close occur error!", mode, uuid, t);
            return null;
        }

    }

    @Override
    public void onError(WebSocket socket, Throwable ex) {
        logger.trace("WEBSOCKET: <<< ERROR;", ex);
        closeF.completeExceptionally(ex);
        try {
            listener.onError(exchangeF.join(), ex);
        } catch (Throwable t) {
            logger.warn("dashscope://exchange/{}/{} fire error occur error!", mode, uuid, t);
        }
    }

}