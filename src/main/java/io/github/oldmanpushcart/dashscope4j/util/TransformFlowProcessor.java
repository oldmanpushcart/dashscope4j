package io.github.oldmanpushcart.dashscope4j.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 流处理转换器
 *
 * @param <T> 输入类型
 * @param <R> 输出类型
 */
public class TransformFlowProcessor<T, R> implements Flow.Processor<T, R> {

    // 订阅端引用锁
    private final AtomicReference<Flow.Subscriber<? super R>> subscriberRef = new AtomicReference<>();

    // 发布端引用锁
    private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();

    // 变速箱
    private final Gearbox<R> gearbox = new Gearbox<>();

    // 转换器
    private final Function<T, List<R>> transformer;

    public TransformFlowProcessor(Function<T, List<R>> transformer) {
        this.transformer = transformer;
    }

    /**
     * 订阅发布端
     * <p>消费端订阅{@code processor}</p>
     *
     * @param subscriber 消费端订阅器
     */
    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {

        // 检查processor发布端是否已经被订阅
        if (!subscriberRef.compareAndSet(null, subscriber)) {
            throw new IllegalStateException("processor publisher already subscribed");
        }

        // 订阅processor
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {

                if (n <= 0) {
                    throw new IllegalArgumentException("non-positive request: %s".formatted(n));
                }

                // 优先消费变速箱中的数据
                final var count = gearbox.polling(n, subscriber::onNext);

                // 变速箱中的数据就已经满足订阅端的需求，则直接返回
                if (count == n) {
                    return;
                }

                // 限流器放开订阅端申请剩余数量
                gearbox.increase(n - count);

                // 向发布端逐个申请数据
                subscriptionRef.get().request(1);

            }

            @Override
            public void cancel() {
                subscriptionRef.get().cancel();
            }

        });

    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

        // 检查processor订阅端是否已经被订阅
        if (!subscriptionRef.compareAndSet(null, subscription)) {
            subscription.cancel();
            throw new IllegalStateException("processor subscriber already subscribed");
        }

    }

    @Override
    public void onNext(T item) {

        // 转换结果先存储到队列中
        transformer.apply(item).forEach(gearbox::offer);

        // 如果队列中有数据，则直接消耗队列中的数据
        if (gearbox.polling(1L, subscriberRef.get()::onNext) == 1L) {
            return;
        }

        // 队列中没有数据，则继续向[订阅端]申请数据
        subscriptionRef.get().request(1);

    }

    @Override
    public void onError(Throwable ex) {
        subscriberRef.get().onError(ex);
    }

    @Override
    public void onComplete() {
        subscriberRef.get().onComplete();
    }

    /**
     * 变速箱
     * <p>用于协调发布端和订阅端因为转换过程中的消费速率不一致</p>
     * <p>
     * 变速箱的核心原理是通过令牌桶算法来控制发布端和订阅端的数据交换速率，
     * 允许发布端不停地向变速箱中放入数据，但订阅端只有在有足够的令牌时才能从变速箱中取出数据。
     * </p>
     *
     * @param <E> 变速箱中的元素类型
     */
    private static class Gearbox<E> {

        private final AtomicLong tokensRef = new AtomicLong(0);
        private final Queue<E> queue = new LinkedList<>();

        /**
         * 是否限速
         * <p>令牌耗尽</p>
         *
         * @return TRUE | FALSE
         */
        public boolean isLimit() {
            return tokensRef.get() <= 0;
        }

        /**
         * 追加令牌
         *
         * @param tokens 追加的令牌数量
         */
        public void increase(long tokens) {
            tokensRef.addAndGet(tokens);
        }

        /**
         * 放入元素
         *
         * @param e 元素
         */
        public void offer(E e) {
            queue.offer(e);
        }

        /**
         * 消费元素
         *
         * @param limit    令牌数量消耗限制
         * @param consumer 消费者
         * @return 消费数量
         */
        public int polling(long limit, Consumer<E> consumer) {
            int count = 0;
            for (; count < limit && !isLimit() && !queue.isEmpty(); count++) {
                consumer.accept(queue.poll());
                tokensRef.decrementAndGet();
            }
            return count;
        }

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
