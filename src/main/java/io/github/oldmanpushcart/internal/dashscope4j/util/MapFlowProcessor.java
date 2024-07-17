package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * 流处理映射器
 *
 * @param <T> 输入类型
 * @param <R> 输出类型
 */
public class MapFlowProcessor<T, R> implements Flow.Processor<T, R> {

    /*
     * 映射函数
     */
    private final BiFunction<T, Throwable, CompletableFuture<List<R>>> mapper;

    /*
     * 用于记录请求的元素数量
     */
    private final AtomicLong requestedRef = new AtomicLong(0);

    /*
     * 用于记录已消费的元素数量
     */
    private final AtomicLong consumedRef = new AtomicLong(0);

    /*
     * 用于请求和消费之间存在的数据数量差，临时存储转换后的元素
     */
    private final Queue<R> queue = new ConcurrentLinkedQueue<>();

    /*
     * 下游引用
     */
    private final AtomicReference<Flow.Subscriber<? super R>> downstreamRef = new AtomicReference<>();

    /*
     * 上游引用
     */
    private final AtomicReference<Flow.Subscription> upstreamRef = new AtomicReference<>();

    /*
     * 已完成标记引用
     * 异常和消费完成均视为已完成
     */
    private final AtomicBoolean isCompletedRef = new AtomicBoolean(false);

    /*
     * 已取消标记引用
     */
    private final AtomicBoolean isCancelledRef = new AtomicBoolean(false);

    /**
     * 构造流处理映射器
     *
     * @param mapper 映射函数
     */
    private MapFlowProcessor(BiFunction<T, Throwable, CompletableFuture<List<R>>> mapper) {
        this.mapper = mapper;
    }

    /**
     * 是否已结束
     * <p>已完成和已取消均视为已结束</p>
     *
     * @return TRUE | FALSE
     */
    private boolean isFinished() {
        return isCompletedRef.get() || isCancelledRef.get();
    }

    /**
     * 是否有处理容量
     * <p>当消费数额小于请求数额时，认为还有数据需要继续消费</p>
     *
     * @return TRUE | FALSE
     */
    private boolean hasRemaining() {
        return requestedRef.get() > consumedRef.get();
    }

    /**
     * 背压处理
     *
     * @param list 转换后的数据
     */
    private void backpressure(List<R> list) {

        // 将转换后的数据首先压入缓冲队列队尾
        if (null != list && !list.isEmpty()) {
            list.forEach(queue::offer);
        }

        // 从缓冲队列中按照队列顺序消费数据
        while (!isFinished() && hasRemaining() && !queue.isEmpty()) {
            downstreamRef.get().onNext(queue.poll());
            consumedRef.incrementAndGet();
        }

        // 如果队列被处理完，但仍然有剩余请求数量，则继续请求上游数据
        if (hasRemaining()) {
            upstreamRef.get().request(1L);
        }

    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> downstream) {

        if (!downstreamRef.compareAndSet(null, downstream)) {
            throw new IllegalStateException("already subscribed!");
        }

        downstream.onSubscribe(new Flow.Subscription() {

            @Override
            public void request(long n) {

                if (n <= 0) {
                    throw new IllegalArgumentException("n must be positive!");
                }

                // 增加请求数，处理long型溢出
                requestedRef.accumulateAndGet(n, (a, b) -> Math.min(a + b, Long.MAX_VALUE));

                // 背压处理数据
                backpressure(null);

            }

            @Override
            public void cancel() {
                if (isCancelledRef.compareAndSet(false, true)) {
                    upstreamRef.get().cancel();
                }
            }

        });
    }

    @Override
    public void onSubscribe(Flow.Subscription upstream) {
        if (!upstreamRef.compareAndSet(null, upstream)) {
            upstream.cancel();
            throw new IllegalStateException("already onSubscribed!");
        }
    }

    @Override
    public void onNext(T item) {

        if (isFinished()) {
            return;
        }

        mapper.apply(item, null)

                // 背压处理数据
                .thenAccept(this::backpressure)

                // 转换过程中出现任何异常，都当成流处理失败
                .whenComplete((r, ex) -> {
                    if (null != ex) {
                        onError(ex);
                    }
                });

    }

    @Override
    public void onError(Throwable ex) {

        if (isFinished()) {
            return;
        }

        mapper.apply(null, ex)

                // 背压处理数据
                .thenAccept(this::backpressure)

                // 转换过程中出现任何异常，都当成流处理失败
                .whenComplete((r, _ex) -> {
                    if (null != _ex) {
                        if (isCompletedRef.compareAndSet(false, true)) {
                            upstreamRef.get().cancel();
                            downstreamRef.get().onError(_ex);
                        }
                    }
                });

    }

    @Override
    public void onComplete() {

        if (isFinished()) {
            return;
        }

        if (isCompletedRef.compareAndSet(false, true)) {
            downstreamRef.get().onComplete();
        }

    }

    /**
     * 映射处理
     *
     * @param source 源数据流
     * @return 映射处理后的数据流
     */
    public Flow.Publisher<R> map(Flow.Publisher<T> source) {
        source.subscribe(this);
        return this;
    }

    /**
     * 异步一对多映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> asyncOneToMany(Flow.Publisher<T> source, BiFunction<T, Throwable, CompletableFuture<List<R>>> mapper) {
        return new MapFlowProcessor<>(mapper)
                .map(source);
    }

    /**
     * 异步一对多映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> asyncOneToMany(Flow.Publisher<T> source, Function<T, CompletableFuture<List<R>>> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> null != ex ? failedFuture(ex) : mapper.apply(t))
                .map(source);
    }

    /**
     * 异步一对一映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> asyncOneToOne(Flow.Publisher<T> source, BiFunction<T, Throwable, CompletableFuture<R>> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> mapper.apply(t, ex).thenApply(List::of))
                .map(source);
    }

    /**
     * 异步一对一映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> asyncOneToOne(Flow.Publisher<T> source, Function<T, CompletableFuture<R>> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> null != ex ? failedFuture(ex) : mapper.apply(t).thenApply(List::of))
                .map(source);
    }

    /**
     * 同步一对多映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> syncOneToMany(Flow.Publisher<T> source, BiFunction<T, Throwable, List<R>> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> completedFuture(mapper.apply(t, ex)))
                .map(source);
    }

    /**
     * 同步一对多映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> syncOneToMany(Flow.Publisher<T> source, Function<T, List<R>> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> null != ex ? failedFuture(ex) : completedFuture(mapper.apply(t)))
                .map(source);
    }

    /**
     * 同步一对一映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> syncOneToOne(Flow.Publisher<T> source, BiFunction<T, Throwable, R> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> completedFuture(List.of(mapper.apply(t, ex))))
                .map(source);
    }

    /**
     * 同步一对一映射
     *
     * @param source 源数据流
     * @param mapper 映射函数
     * @param <T>    源类型
     * @param <R>    目标类型
     * @return 映射处理后的数据流
     */
    public static <T, R> Flow.Publisher<R> syncOneToOne(Flow.Publisher<T> source, Function<T, R> mapper) {
        return new MapFlowProcessor<T, R>((t, ex) -> null != ex ? failedFuture(ex) : completedFuture(List.of(mapper.apply(t))))
                .map(source);
    }

}
