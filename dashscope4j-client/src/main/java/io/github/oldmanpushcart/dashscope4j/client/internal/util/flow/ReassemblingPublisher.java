package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 碎片流处理
 * <p>
 * 碎片流处理，将碎片流处理成完整的事件流
 * </p>
 *
 * @param <T> 碎片类型
 * @param <R> 完整事件类型
 */
public class ReassemblingPublisher<T, R> implements Flow.Publisher<R> {

    private final Flow.Publisher<T> upstream;
    private final Reassembler<T, R> reassembler;

    /**
     * 碎片流处理构造函数
     *
     * @param upstream    碎片流
     * @param reassembler 碎片组装器
     */
    public ReassemblingPublisher(Flow.Publisher<T> upstream, Reassembler<T, R> reassembler) {
        this.upstream = upstream;
        this.reassembler = reassembler;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        upstream.subscribe(new ReassemblingSubscriber<>(subscriber, reassembler));
    }

    /**
     * 碎片订阅者
     * <p>
     * 碎片订阅者，将碎片流处理成完整事件流
     * </p>
     */
    private static class ReassemblingSubscriber<T, R> implements Flow.Subscriber<T> {

        private final Flow.Subscriber<? super R> downstream;
        private final Reassembler<T, R> reassembler;

        private final Queue<R> queue = new ConcurrentLinkedQueue<>();
        private final AtomicLong demandedAtomic = new AtomicLong(0);
        private final AtomicBoolean doneAtomic = new AtomicBoolean(false);
        private final AtomicInteger wip = new AtomicInteger(0);

        private volatile Flow.Subscription subscription;

        private ReassemblingSubscriber(Flow.Subscriber<? super R> downstream, Reassembler<T, R> reassembler) {
            this.downstream = downstream;
            this.reassembler = reassembler;
        }

        /**
         * 发送事件
         *
         * @param items 事件列表
         */
        private void emit(List<R> items) {
            items.forEach(queue::offer);
            drain();
        }

        /**
         * 刷新碎片
         */
        private void drain() {

            // 只允许一个线程处理事件
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            do {

                /*
                 * 发送下游已请求的数据量
                 * 有多少处理多少，直到事件队列为空
                 */
                final var demanded = demandedAtomic.get();
                long emitted = 0L;
                while (emitted < demanded) {
                    final var event = queue.poll();
                    if (null == event) {
                        break;
                    }
                    downstream.onNext(event);
                    emitted++;
                }
                if (emitted > 0L) {
                    demandedAtomic.addAndGet(-emitted);
                }

                /*
                 * 如果流处理已结束，而且当前队列中已无任何事件需要处理，
                 * 则通知整个流已完成
                 */
                if (doneAtomic.get() && queue.isEmpty()) {
                    downstream.onComplete();
                    return;
                }

                /*
                 * 在这里完成 missed 的结算
                 * 根据 WIP 机制计算出在消费的这段过程中又有多少个线程并发进入
                 */
                missed = wip.addAndGet(-missed);

                // 如果流处理未结束，且当前队列已无事件需要处理，则请求1个碎片
                final var shouldRequestMore = !doneAtomic.get()
                        && demandedAtomic.get() > 0
                        && queue.isEmpty();
                if (shouldRequestMore) {
                    final var s = subscription;
                    if (null != s) {
                        s.request(1);
                    }
                }

            } while (missed != 0);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            downstream.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {

                    if (n <= 0) {
                        onError(new IllegalArgumentException("n must be positive"));
                        return;
                    }

                    // 累加请求，防溢出
                    demandedAtomic.accumulateAndGet(n, (cur, req) ->
                            cur == Long.MAX_VALUE || cur + req < 0 ? Long.MAX_VALUE : cur + req);

                    // 启动数据处理
                    drain();

                }

                @Override
                public void cancel() {
                    if (!doneAtomic.compareAndSet(false, true)) {
                        return;
                    }
                    subscription.cancel();
                }
            });
        }

        @Override
        public void onNext(T item) {
            if (doneAtomic.get()) {
                return;
            }
            try {
                emit(reassembler.tryAssemble(item));
            } catch (Throwable nextEx) {
                onError(nextEx);
            }
        }

        @Override
        public void onError(Throwable ex) {
            if (!doneAtomic.compareAndSet(false, true)) {
                return;
            }
            downstream.onError(ex);
        }

        @Override
        public void onComplete() {
            if (!doneAtomic.compareAndSet(false, true)) {
                return;
            }
            try {
                emit(reassembler.flush());
            } catch (Throwable completeEx) {
                onError(completeEx);
            }
        }

    }


    /**
     * 碎片组装器
     *
     * @param <T> 碎片类型
     * @param <R> 组装后的事件类型
     */
    public interface Reassembler<T, R> {

        /**
         * 尝试组装碎片
         * <p>
         * 将碎片组装成1个或多个完整的事件
         * </p>
         *
         * @param item 碎片
         * @return 组装后的事件集
         */
        List<R> tryAssemble(T item);

        /**
         * 当流结束时，尝试 flush 剩余缓冲区
         */
        default List<R> flush() {
            return Collections.emptyList();
        }

    }

}
