package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FlatMapPublisher<T, R> implements Flow.Publisher<R> {

    private final Flow.Publisher<T> upstream;
    private final Function<T, Flow.Publisher<R>> each;
    private final Supplier<Flow.Publisher<R>> fin;

    public FlatMapPublisher(Flow.Publisher<T> upstream, FlatMapper<T, R> mapper) {
        this(upstream, mapper::map, mapper::finish);
    }

    public FlatMapPublisher(Flow.Publisher<T> upstream, Function<T, Flow.Publisher<R>> each, Supplier<Flow.Publisher<R>> fin) {
        this.upstream = Objects.requireNonNull(upstream);
        this.each = Objects.requireNonNull(each);
        this.fin = Objects.requireNonNull(fin);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> downstream) {
        Objects.requireNonNull(downstream);
        upstream.subscribe(new UpstreamSubscriber<>(downstream, each, fin));
    }

    private enum State {
        SUBSCRIBING,
        SUBSCRIBED,
        READY,
        INNER_SUBSCRIBING,
        INNER_SUBSCRIBED,
        INNER_READY,
        INNER_COMPLETED,
        ERROR,
        COMPLETED,
        CANCELLED
    }

    private static class Quota {
        private final AtomicLong requested = new AtomicLong(0);
        private final AtomicLong emitted = new AtomicLong(0);

        private static void safetyAccumulateAndGet(AtomicLong atomicLong, long delta) {
            atomicLong.accumulateAndGet(delta, (cur, req) ->
                    cur == Long.MAX_VALUE || cur + req < 0 ? Long.MAX_VALUE : cur + req);
        }

        public void requested(long delta) {
            safetyAccumulateAndGet(requested, delta);
        }


        public void emitted(long delta) {
            safetyAccumulateAndGet(emitted, delta);
        }

        public long available() {
            final var r = requested.get();
            final var e = emitted.get();
            return r == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, r - e);
        }

    }

    private static class EmptySubscription implements Flow.Subscription {

        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }

    }

    private static class UpstreamSubscriber<T, R> implements Flow.Subscriber<T> {

        private final Flow.Subscriber<? super R> downstream;
        private final Function<T, Flow.Publisher<R>> each;
        private final Supplier<Flow.Publisher<R>> fin;

        private final AtomicInteger wip = new AtomicInteger(0);
        private final AtomicReference<State> state = new AtomicReference<>(State.SUBSCRIBING);
        private final Quota quota = new Quota();
        private final Quota upstreamQuota = new Quota();
        private final Queue<Flow.Publisher<R>> queue = new ConcurrentLinkedQueue<>();

        private volatile Flow.Subscription upstreamSubscription;
        private volatile InnerSubscriber<R> innerSubscriber;
        private volatile boolean upstreamCompleted;
        private volatile boolean terminated;
        private volatile Throwable ex;

        private UpstreamSubscriber(Flow.Subscriber<? super R> downstream, Function<T, Flow.Publisher<R>> each, Supplier<Flow.Publisher<R>> fin) {
            this.downstream = downstream;
            this.each = each;
            this.fin = fin;
        }

        private void select() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            do {

                try {

                    final var s = state.get();
                    switch (s) {

                        // 主流已订阅
                        case SUBSCRIBED -> {
                            if (state.compareAndSet(s, State.READY)) {
                                upstreamSubscription.request(1L);
                            }
                        }

                        /*
                         * 主流准备完成
                         * 在这里完成主流的所有数据处理
                         */
                        case READY -> {

                            /*
                             * 消费已经存在的数据
                             * 每个主流的数据将会申请一个子流来进行消费，后续的执行将切换到子流的状态流转进行
                             */
                            if (!queue.isEmpty()) {
                                if (state.compareAndSet(s, State.INNER_SUBSCRIBING)) {
                                    final var innerPub = Objects.requireNonNull(queue.poll());
                                    final var is = new InnerSubscriber<R>(state, upstreamCompleted, this::select, this::onError);
                                    innerSubscriber = is;
                                    innerPub.subscribe(is);
                                }
                                break;
                            }

                            /*
                             * 数据消费完成后，检查主流是否已经完结。
                             * 主流完结就可以走到FIN子流，后续的状态推进将由FIN子流进行
                             */
                            if (upstreamCompleted) {
                                final var innerPub = Objects.requireNonNull(fin.get());
                                queue.offer(innerPub);
                                select();
                                break;
                            }

                            /*
                             * 当前主流配额耗尽，则需要重新申请新的配额
                             */
                            if (upstreamQuota.available() == 0) {
                                upstreamSubscription.request(1L);
                            }

                        }

                        // 子流订阅完成
                        case INNER_SUBSCRIBED -> {
                            if (state.compareAndSet(s, State.INNER_READY)) {
                                final var n = quota.available();
                                if (n > 0) {
                                    innerSubscriber.subscription.request(n);
                                }
                            }
                        }

                        // 子流准备完成
                        case INNER_READY -> {
                            final var is = innerSubscriber;

                            /*
                             * 子流逐个消费子流数据
                             * 每消费一个数据就重新通过select走回到这里。
                             *
                             * 这样设计的逻辑是希望能通过select的状态判断，当状态发生改变时能立即停止子流的数据消费。
                             */
                            if (!is.queue.isEmpty()) {
                                final var r = Objects.requireNonNull(is.queue.poll());
                                quota.emitted(1L);
                                downstream.onNext(r);
                                select();
                                break;
                            }

                            // 子流未结束但子流配额消费完成，重新请求新的配额
                            if (is.quota.available() == 0) {
                                final var n = quota.available();
                                if (n > 0) {
                                    is.subscription.request(n);
                                }
                            }
                        }


                        case INNER_COMPLETED -> {
                            final var is = innerSubscriber;

                            // 刷走子流中未发送的元素
                            if (!is.queue.isEmpty()) {
                                final var r = Objects.requireNonNull(is.queue.poll());
                                quota.emitted(1L);
                                downstream.onNext(r);
                                select();
                                break;
                            }

                            // FIN子流完成，是整个流处理完成。
                            if (is.terminal) {
                                if (state.compareAndSet(s, State.COMPLETED)) {
                                    select();
                                }
                            }

                            // 子流完成，切回到主流的READY，让主流READY继续完成后续判断
                            else {
                                if (state.compareAndSet(s, State.READY)) {
                                    select();
                                }
                            }
                        }

                        // 终结态
                        case COMPLETED, ERROR, CANCELLED -> {
                            try {
                                terminated = true;

                                // 取消上游
                                final var us = upstreamSubscription;
                                if (null != us) {
                                    us.cancel();
                                }

                                // 取消子流
                                final var is = innerSubscriber;
                                if (null != is && null != is.subscription) {
                                    is.subscription.cancel();
                                }

                                // 根据是否有错误决定是完成还是失败
                                if (null != ex) {
                                    downstream.onError(ex);
                                } else {
                                    downstream.onComplete();
                                }

                            } catch (Throwable ex) {
                                // ignored
                            }
                            return;
                        }

                    }

                } catch (Throwable ex) {
                    onError(ex);
                }

                missed = wip.addAndGet(-missed);
            } while (missed != 0 && !terminated);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {

            if (!state.compareAndSet(State.SUBSCRIBING, State.SUBSCRIBED)) {
                s.cancel();
                downstream.onSubscribe(new EmptySubscription());
                downstream.onError(new IllegalStateException("Publisher already subscribed"));
            }

            upstreamSubscription = new Flow.Subscription() {
                @Override
                public void request(long n) {
                    upstreamQuota.requested(n);
                    s.request(n);
                }

                @Override
                public void cancel() {
                    s.cancel();
                }
            };
            downstream.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        onError(new IllegalArgumentException("n must be positive"));
                        return;
                    }
                    quota.requested(n);
                    select();
                }

                @Override
                public void cancel() {
                    state.set(State.CANCELLED);
                    select();
                }
            });
            select();
        }

        @Override
        public void onNext(T t) {
            try {
                upstreamQuota.emitted(1L);
                final var innerPub = Objects.requireNonNull(each.apply(t));
                queue.offer(innerPub);
                select();
            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            this.ex = ex;
            state.set(State.ERROR);
            select();
        }

        @Override
        public void onComplete() {
            upstreamCompleted = true;
            select();
        }

    }

    /**
     * 子流订阅者
     */
    private static class InnerSubscriber<R> implements Flow.Subscriber<R> {

        private final AtomicReference<State> state;
        private final boolean terminal;
        private final Runnable select;
        private final Consumer<Throwable> onError;
        private final Queue<R> queue = new ConcurrentLinkedQueue<>();
        private final Quota quota = new Quota();
        private volatile Flow.Subscription subscription;

        private InnerSubscriber(AtomicReference<State> state, boolean terminal, Runnable select, Consumer<Throwable> onError) {
            this.state = state;
            this.terminal = terminal;
            this.select = select;
            this.onError = onError;
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            subscription = new Flow.Subscription() {

                @Override
                public void request(long n) {
                    quota.requested(n);
                    s.request(n);
                }

                @Override
                public void cancel() {
                    s.cancel();
                }

            };
            if (state.compareAndSet(State.INNER_SUBSCRIBING, State.INNER_SUBSCRIBED)) {
                select.run();
            }
        }

        @Override
        public void onNext(R r) {
            quota.emitted(1L);
            queue.offer(r);
            select.run();
        }

        @Override
        public void onError(Throwable ex) {
            onError.accept(ex);
        }

        @Override
        public void onComplete() {
            state.set(State.INNER_COMPLETED);
            select.run();
        }

    }


    @FunctionalInterface
    public interface FlatMapper<T, R> {

        /**
         * 将上游元素映射为子流。
         */
        Flow.Publisher<R> map(T t);

        /**
         * 提供终结流（主流 onComplete 后追加的内容）。
         * 默认为空流。
         *
         * <p>注意：此方法应在需要时才被调用（例如主流正常完成且未取消）。
         * 实现应尽量轻量，或返回可重复订阅的 Publisher。
         */
        default Flow.Publisher<R> finish() {
            return EmptyPublisher.empty();
        }

    }

}
