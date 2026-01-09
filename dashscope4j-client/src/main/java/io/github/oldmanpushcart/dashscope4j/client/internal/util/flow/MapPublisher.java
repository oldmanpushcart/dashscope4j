package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class MapPublisher<T, R> implements Flow.Publisher<R> {

    private final Flow.Publisher<T> upstream;
    private final Function<T, Flow.Publisher<R>> map;

    public MapPublisher(Flow.Publisher<T> upstream, Function<T, Flow.Publisher<R>> map) {
        Objects.requireNonNull(upstream, "upstream must not be null!");
        Objects.requireNonNull(map, "map must not be null!");
        this.upstream = upstream;
        this.map = map;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> downstream) {
        Objects.requireNonNull(downstream, "downstream must not be null!");
        upstream.subscribe(new UpstreamSubscriber<>(downstream, map));
    }

    private enum State {
        SUBSCRIBING,
        SUBSCRIBED,
        MAIN_READY,
        MAIN_COMPLETED,
        INNER_SUBSCRIBING,
        INNER_SUBSCRIBED,
        INNER_READY,
        INNER_COMPLETED,
        ERROR,
        COMPLETED,
        CANCELLED
    }

    private static class UpstreamSubscriber<T, R> implements Flow.Subscriber<T> {

        private final Flow.Subscriber<? super R> downstream;
        private final Function<T, Flow.Publisher<R>> map;

        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicReference<State> state = new AtomicReference<>(State.SUBSCRIBING);
        private final Quota quota = new Quota();
        private final Queue<Flow.Publisher<R>> queue = new ConcurrentLinkedQueue<>();

        private volatile boolean terminated;
        private volatile Flow.Subscription upstreamSubscription;
        private volatile InnerSubscriber<R> innerSubscriber;
        private volatile Throwable ex;

        private UpstreamSubscriber(Flow.Subscriber<? super R> downstream, Function<T, Flow.Publisher<R>> map) {
            this.downstream = downstream;
            this.map = map;
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
                            if (state.compareAndSet(s, State.MAIN_READY)) {
                                upstreamSubscription.request(1L);
                            }
                        }

                        // 主流准备完成
                        case MAIN_READY -> {

                            // 优先执行队列中的数据
                            if (!queue.isEmpty()) {
                                if (state.compareAndSet(s, State.INNER_SUBSCRIBING)) {
                                    final var innerPub = Objects.requireNonNull(queue.poll());
                                    final var is = new InnerSubscriber<R>(state, this::select, this::onError);
                                    innerSubscriber = is;
                                    innerPub.subscribe(is);
                                }
                                break;
                            }

                            // 队列执行完成，说明空了，继续向上游请求
                            upstreamSubscription.request(1L);

                        }

                        // 主流已完成
                        case MAIN_COMPLETED -> {
                            if (state.compareAndSet(s, State.COMPLETED)) {
                                select();
                            }
                        }

                        // 子流已订阅
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

                            // 子流有数据，发送给下游
                            if (!is.queue.isEmpty()) {
                                final var r = Objects.requireNonNull(is.queue.poll());
                                quota.emitted(1L);
                                downstream.onNext(r);
                                select();
                                break;
                            }

                            // 子流配额耗尽，重新向子流拉取数据
                            if (is.quota.available() == 0) {
                                final var n = quota.available();
                                if (n > 0) {
                                    is.subscription.request(n);
                                }
                            }

                        }

                        // 子流已完成
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

                            /*
                             * 子流完成，从哪里来回哪里去
                             * 从MAIN_READY来，回MAIN_READY去
                             */
                            if (state.compareAndSet(s, State.MAIN_READY)) {
                                select();
                            }

                        }


                        // 终结态
                        case COMPLETED, ERROR, CANCELLED -> {
                            try {

                                // 标记为终结
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

                            /*
                             * 进入终结态后，后续不会再有任何的 select
                             * 所以这里可以通过 return 快速破坏执行链，起到立即结束的作用
                             */
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
            Objects.requireNonNull(s, "subscription must not be null!");
            if (!state.compareAndSet(State.SUBSCRIBING, State.SUBSCRIBED)) {
                s.cancel();
                downstream.onSubscribe(new EmptySubscription());
                downstream.onError(new IllegalStateException("Already subscribed!"));
                return;
            }
            upstreamSubscription = new Flow.Subscription() {
                @Override
                public void request(long n) {
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
                        onError(new IllegalArgumentException("n must be positive!"));
                        return;
                    }
                    quota.requested(n);
                }

                @Override
                public void cancel() {
                    state.accumulateAndGet(State.CANCELLED, (s, u) ->
                            switch (s) {
                                case ERROR, COMPLETED, CANCELLED -> s;
                                default -> u;
                            });
                    select();
                }
            });
            select();
        }

        @Override
        public void onNext(T t) {
            try {
                final var innerPub = map.apply(t);
                queue.offer(innerPub);
                select();
            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            this.ex = ex;
            final var ret = state.accumulateAndGet(State.ERROR, (s, u) ->
                    switch (s) {
                        case ERROR, COMPLETED, CANCELLED -> s;
                        default -> u;
                    });
            if (ret == State.ERROR) {
                select();
            }
        }

        @Override
        public void onComplete() {
            final var ret = state.accumulateAndGet(State.MAIN_COMPLETED, (s, u) ->
                    switch (s) {
                        case ERROR, COMPLETED, CANCELLED -> s;
                        default -> u;
                    });
            if (ret == State.MAIN_COMPLETED) {
                select();
            }
        }

    }

    private static class InnerSubscriber<R> implements Flow.Subscriber<R> {

        private final AtomicReference<State> state;
        private final Runnable select;
        private final Consumer<Throwable> onError;

        private final Queue<R> queue = new ConcurrentLinkedQueue<>();
        private final Quota quota = new Quota();

        private volatile Flow.Subscription subscription;

        private InnerSubscriber(AtomicReference<State> state, Runnable select, Consumer<Throwable> onError) {
            this.state = state;
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
            final var ret = state.accumulateAndGet(State.INNER_COMPLETED, (s, u) ->
                    switch (s) {
                        case INNER_SUBSCRIBING, INNER_SUBSCRIBED, INNER_READY -> u;
                        default -> s;
                    });
            if (ret == State.INNER_COMPLETED) {
                select.run();
            }
        }

    }

}
