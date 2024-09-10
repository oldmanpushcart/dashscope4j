package io.github.oldmanpushcart.dashscope4j.base.exchange;

import io.github.oldmanpushcart.internal.dashscope4j.util.IOUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 数据交换监听器集合
 *
 * @since 2.2.0
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
        requireNonNull(consumer);
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
     * 消费数据流
     *
     * @param consumer 消费者
     * @param <T>      流入数据类型
     * @param <R>      流出数据类型
     * @return 监听器
     * @since 2.2.1
     */
    public static <T, R> Exchange.Listener<T, R> ofConsume(BiConsumer<R, Throwable> consumer) {
        requireNonNull(consumer);
        return new Exchange.Listener<>() {

            @Override
            public CompletionStage<?> onData(Exchange<T, R> exchange, R data) {
                consumer.accept(data, null);
                exchange.request(1);
                return DONE;
            }

            @Override
            public void onError(Exchange<T, R> exchange, Throwable ex) {
                consumer.accept(null, ex);
            }

        };
    }

    /**
     * 消费字节流到数据通道
     *
     * @param channel 数据写入通道
     * @param <T>     流入数据类型
     * @param <R>     流出数据类型
     * @return 监听器
     */
    public static <T, R> Exchange.Listener<T, R> ofByteChannel(WritableByteChannel channel) {
        return ofByteChannel(channel, false);
    }

    /**
     * 消费字节流到数据通道
     *
     * @param channel   数据写入通道
     * @param autoClose 是否自动关闭
     * @param <T>       流入数据类型
     * @param <R>       流出数据类型
     * @return 监听器
     * @since 2.2.1
     */
    public static <T, R> Exchange.Listener<T, R> ofByteChannel(WritableByteChannel channel, boolean autoClose) {
        requireNonNull(channel);
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

            @Override
            public CompletionStage<?> onCompleted(Exchange<T, R> exchange, int status, String reason) {
                closeQuietly();
                return Exchange.Listener.super.onCompleted(exchange, status, reason);
            }

            @Override
            public void onError(Exchange<T, R> exchange, Throwable ex) {
                closeQuietly();
                Exchange.Listener.super.onError(exchange, ex);
            }

            private void closeQuietly() {
                if (autoClose) {
                    IOUtils.closeQuietly(channel);
                }
            }

        };
    }

    /**
     * 消费字节流到{@link Path}
     *
     * @param path    Path
     * @param options Open Options
     * @param <T>     流入数据类型
     * @param <R>     流出数据类型
     * @return 监听器
     * @throws IOException 打开{@link Path}失败
     */
    public static <T, R> Exchange.Listener<T, R> ofPath(Path path, OpenOption... options) throws IOException {
        requireNonNull(path);
        return ofByteChannel(FileChannel.open(path, options), true);
    }

    /**
     * 消费字节流到{@link Path}
     *
     * @param path Path
     * @param <T>  流入数据类型
     * @param <R>  流出数据类型
     * @return 监听器
     * @throws IOException 打开{@link Path}失败
     */
    public static <T, R> Exchange.Listener<T, R> ofPath(Path path) throws IOException {
        requireNonNull(path);
        return ofPath(path, CREATE, WRITE);
    }

}
