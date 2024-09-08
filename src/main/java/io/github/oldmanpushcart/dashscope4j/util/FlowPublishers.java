package io.github.oldmanpushcart.dashscope4j.util;

import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.github.oldmanpushcart.internal.dashscope4j.util.IOUtils.closeQuietly;

/**
 * 流-发布器-集合
 */
public class FlowPublishers {

    /**
     * {@code T[] -> Flow.Publisher<T>}
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 发布器
     */
    public static <T> Flow.Publisher<T> fromArray(T[] array) {
        Objects.requireNonNull(array);
        return fromIterator(List.of(array));
    }

    /**
     * {@code Iterable<T> -> Flow.Publisher<T>}
     *
     * @param iterable 迭代器
     * @param <T>      元素类型
     * @return 发布器
     */
    public static <T> Flow.Publisher<T> fromIterator(Iterable<T> iterable) {
        Objects.requireNonNull(iterable);
        return subscriber -> subscriber.onSubscribe(new Flow.Subscription() {

            private final Iterator<T> iterator = iterable.iterator();
            private final AtomicBoolean isFinishRef = new AtomicBoolean(false);

            @Override
            public void request(long n) {
                if (isFinishRef.get() || n <= 0) {
                    return;
                }
                while (iterator.hasNext() && n > 0) {
                    subscriber.onNext(iterator.next());
                    n--;
                }
                if (!iterator.hasNext() && isFinishRef.compareAndSet(false, true)) {
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
                if (isFinishRef.compareAndSet(false, true)) {
                    subscriber.onError(new CancellationException());
                }
            }

        });
    }

    /**
     * 映射发布器
     *
     * @param publisher 原始发布器
     * @param mapper    映射函数
     * @param <T>       原始元素类型
     * @param <R>       映射元素类型
     * @return 发布器
     */
    public static <T, R> Flow.Publisher<R> mapOneToOne(Flow.Publisher<T> publisher, Function<T, R> mapper) {
        Objects.requireNonNull(publisher);
        Objects.requireNonNull(mapper);
        return subscriber -> publisher.subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(T item) {
                subscriber.onNext(mapper.apply(item));
            }

            @Override
            public void onError(Throwable ex) {
                subscriber.onError(ex);
            }

            @Override
            public void onComplete() {
                subscriber.onComplete();
            }

        });
    }

    /**
     * {@code ReadableByteChannel -> Flow.Publisher<ByteBuffer>}
     *
     * @param channel    流入 ByteBuffer 通道
     * @param bufferSize 缓存块大小
     * @return 发布器
     */
    public static Flow.Publisher<ByteBuffer> fromByteChannel(ReadableByteChannel channel, int bufferSize) {
        Objects.requireNonNull(channel);
        CommonUtils.check(bufferSize, v -> v > 0, "bufferSize must be greater than 0");
        return subscriber -> subscriber.onSubscribe(new Flow.Subscription() {

            private final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            private final AtomicBoolean isFinishRef = new AtomicBoolean(false);

            @Override
            public void request(long n) {

                if (isFinishRef.get() || n <= 0) {
                    return;
                }

                try {
                    while (n-- > 0 && !isFinishRef.get()) {
                        buffer.clear();
                        if (channel.read(buffer) == -1) {
                            if (isFinishRef.compareAndSet(false, true)) {
                                subscriber.onComplete();
                            }
                            break;
                        }
                        buffer.flip();
                        subscriber.onNext(buffer);
                    }
                } catch (Exception ex) {
                    if (isFinishRef.compareAndSet(false, true)) {
                        subscriber.onError(ex);
                    }
                }

            }

            @Override
            public void cancel() {
                if (isFinishRef.compareAndSet(false, true)) {
                    subscriber.onError(new CancellationException());
                }
            }

        });
    }

    /**
     * {@code ReadableByteChannel -> Flow.Publisher<ByteBuffer>}
     *
     * @param channel 流入 ByteBuffer 通道
     * @return 发布器
     */
    public static Flow.Publisher<ByteBuffer> fromByteChannel(ReadableByteChannel channel) {
        Objects.requireNonNull(channel);
        return fromByteChannel(channel, 4 * 1024);
    }

    /**
     * {@code URI -> Flow.Publisher<ByteBuffer>}
     *
     * @param resource 资源 URI 地址
     * @return 发布器
     * @throws IOException 打开资源失败
     */
    public static Flow.Publisher<ByteBuffer> fromURI(URI resource) throws IOException {
        Objects.requireNonNull(resource);
        final var channel = Channels.newChannel(resource.toURL().openStream());
        return subscriber -> fromByteChannel(channel).subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscriber.onSubscribe(subscription);
            }

            @Override
            public void onNext(ByteBuffer item) {
                subscriber.onNext(item);
            }

            @Override
            public void onError(Throwable ex) {
                closeQuietly(channel);
                subscriber.onError(ex);
            }

            @Override
            public void onComplete() {
                closeQuietly(channel);
                subscriber.onComplete();
            }

        });
    }

}
