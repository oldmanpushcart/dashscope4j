package io.github.oldmanpushcart.dashscope4j.util;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 流处理转换器
 *
 * @param <T> 输入类型
 * @param <R> 输出类型
 */
public class TransformFlowProcessor<T, R> implements Flow.Processor<T, R> {

    private final Function<T, List<R>> transformer;
    private final Queue<R> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong requestedRef = new AtomicLong(0);
    private final AtomicLong producedRef = new AtomicLong(0);
    private final AtomicReference<Flow.Subscriber<? super R>> downstreamRef = new AtomicReference<>();
    private final AtomicReference<Flow.Subscription> upstreamRef = new AtomicReference<>();
    private volatile boolean isCompleted = false;
    private volatile boolean isCancelled = false;

    public TransformFlowProcessor(Function<T, List<R>> transformer) {
        this.transformer = transformer;
    }

    private boolean isFinished() {
        return isCompleted && isCancelled;
    }

    private boolean isBehind() {
        return requestedRef.get() > producedRef.get();
    }

    private long polling(long n) {
        long completed = 0;
        try {
            while (!isFinished() && isBehind() && !queue.isEmpty() && completed <= n) {
                downstreamRef.get().onNext(queue.poll());
                producedRef.incrementAndGet();
                completed++;
            }
        } catch (Throwable t) {
            onError(t);
        }
        return completed;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> downstream) {
        if (!downstreamRef.compareAndSet(null, downstream)) {
            throw new IllegalStateException("already subscribed!");
        }
        downstream.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                requestedRef.addAndGet(n);
                final var inc = n - polling(n);
                if (inc > 0) {
                    upstreamRef.get().request(inc);
                }
            }

            @Override
            public void cancel() {
                isCancelled = true;
                upstreamRef.get().cancel();
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
        try {
            transformer.apply(item).forEach(queue::offer);
            polling(1);
        } catch (Throwable t) {
            onError(t);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (isFinished()) {
            return;
        }
        isCompleted = true;
        upstreamRef.get().cancel();
        downstreamRef.get().onError(t);
        onFinished();
    }

    @Override
    public void onComplete() {
        if (isFinished()) {
            return;
        }
        isCompleted = true;
        downstreamRef.get().onComplete();
        onFinished();
    }

    protected void onFinished() {

    }

    /**
     * 转换{@link Flow.Publisher}，从{@code <T>}转换为{@code <R>}
     *
     * @param source 源
     * @return 目标
     */
    public Flow.Publisher<R> transform(Flow.Publisher<T> source) {
        source.subscribe(this);
        return this;
    }

    /**
     * 转换{@link Flow.Publisher}，从{@code <T>}转换为{@code <R>}
     *
     * @param source      源
     * @param transformer 转换器
     * @param <T>         源类型
     * @param <R>         目标类型
     * @return 目标
     */
    public static <T, R> Flow.Publisher<R> transform(Flow.Publisher<T> source, Function<T, List<R>> transformer) {
        return new TransformFlowProcessor<>(transformer).transform(source);
    }

}
