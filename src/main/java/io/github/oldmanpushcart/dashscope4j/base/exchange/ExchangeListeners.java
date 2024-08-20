package io.github.oldmanpushcart.dashscope4j.base.exchange;

import io.github.oldmanpushcart.internal.dashscope4j.base.exchange.ProxyExchangeListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static io.github.oldmanpushcart.internal.dashscope4j.util.IOUtils.closeQuietly;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 交互通道监听器集合
 */
public final class ExchangeListeners {

    private static final CompletableFuture<?> DONE = completedFuture(null);

    private ExchangeListeners() {
    }

    /**
     * 消费数据流
     *
     * @param consumer 消费者
     * @param <T>      流入数据类型
     * @param <R>      流出数据类型
     * @return 监听器
     */
    public static <T, R> Exchange.Listener<T, R> ofConsume(Consumer<R> consumer) {
        Objects.requireNonNull(consumer);
        return new Exchange.Listener<>() {

            @Override
            public CompletionStage<?> onData(Exchange<T, R> exchange, R data) {
                consumer.accept(data);
                exchange.request(1);
                return DONE;
            }

        };
    }

    /**
     * 消费 ByteBuffer 流
     *
     * @param channel 流出ByteBuffer通道
     * @param <T>     流入数据类型
     * @param <R>     流出数据类型
     * @return 监听器
     */
    public static <T, R> Exchange.Listener<T, R> ofByteChannel(WritableByteChannel channel) {
        Objects.requireNonNull(channel);
        return new Exchange.Listener<>() {

            @Override
            public CompletionStage<?> onByteBuffer(Exchange<T, R> exchange, ByteBuffer buf, boolean last) {
                try {
                    while (buf.hasRemaining()) {
                        channel.write(buf);
                    }
                } catch (IOException ioEx) {
                    throw new CompletionException("write buf error!", ioEx);
                }
                exchange.request(1);
                return DONE;
            }

        };
    }

    /**
     * 消费ByteBuffer流到{@link Path}
     *
     * @param path    Path
     * @param options Open Options
     * @param <T>     流入数据类型
     * @param <R>     流出数据类型
     * @return 监听器
     * @throws IOException 打开{@link Path}失败
     */
    public static <T, R> Exchange.Listener<T, R> ofPath(Path path, OpenOption... options) throws IOException {
        @SuppressWarnings("resource") final var channel = FileChannel.open(path, options);
        return new ProxyExchangeListener<>(ofByteChannel(channel)) {

            @Override
            public void onOpen(Exchange<T, R> exchange) {
                exchange.closeFuture()
                        .whenComplete((v, ex) -> closeQuietly(channel));
                super.onOpen(exchange);
            }

        };
    }

    /**
     * 消费ByteBuffer流到{@link Path}
     *
     * @param path Path
     * @param <T>  流入数据类型
     * @param <R>  流出数据类型
     * @return 监听器
     * @throws IOException 打开{@link Path}失败
     */
    public static <T, R> Exchange.Listener<T, R> ofPath(Path path) throws IOException {
        return ofPath(path, CREATE, WRITE);
    }

}
