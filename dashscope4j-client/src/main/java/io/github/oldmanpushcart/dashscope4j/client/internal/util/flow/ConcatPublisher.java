package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

class ConcatPublisher<T> implements Flow.Publisher<T> {

    private final Flow.Publisher<T> upstream;
    private final Flow.Publisher<T> fin;
    private final Function<Throwable, Flow.Publisher<T>> err;

    public ConcatPublisher(Flow.Publisher<T> upstream, Flow.Publisher<T> fin, Function<Throwable, Flow.Publisher<T>> err) {
        Objects.requireNonNull(upstream, "upstream must not be null!");
        Objects.requireNonNull(fin, "next must not be null!");
        Objects.requireNonNull(err, "err must not be null!");
        this.upstream = upstream;
        this.fin = fin;
        this.err = err;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super T> downstream) {
        Objects.requireNonNull(downstream, "downstream must not be null!");
        upstream.subscribe(new UpstreamSubscriber<>(downstream, fin, err));
    }

    private enum State {
        SUBSCRIBING,
        SUBSCRIBED,
        MAIN_READY,
        MAIN_COMPLETED,
        MAIN_ERROR,
        INNER_SUBSCRIBING,
        INNER_SUBSCRIBED,
        INNER_READY,
        INNER_COMPLETED,
        INNER_ERROR,
        ERROR,
        COMPLETED,
        CANCELLED
    }

    private static class UpstreamSubscriber<T> implements Flow.Subscriber<T> {

        private final Flow.Subscriber<? super T> downstream;
        private final Flow.Publisher<T> fin;
        private final Function<Throwable, Flow.Publisher<T>> err;

        private final AtomicReference<State> state = new AtomicReference<>(State.SUBSCRIBING);
        private final AtomicInteger wip = new AtomicInteger();
        private final Quota quota = new Quota();

        private volatile boolean terminated;
        private volatile Flow.Subscription upstreamSubscription;
        private volatile InnerSubscriber<T> innerSubscriber;
        private volatile Throwable ex;

        private UpstreamSubscriber(Flow.Subscriber<? super T> downstream, Flow.Publisher<T> fin, Function<Throwable, Flow.Publisher<T>> err) {
            this.downstream = downstream;
            this.fin = fin;
            this.err = err;
        }

        private void select() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            do {

                final var s = state.get();
                try {
                    switch (s) {
                        case MAIN_COMPLETED -> {
                            if (state.compareAndSet(s, State.INNER_SUBSCRIBING)) {
                                final var is = new InnerSubscriber<T>(state, this::select);
                                innerSubscriber = is;
                                fin.subscribe(is);
                            }
                        }

                        case MAIN_ERROR -> {
                            if (state.compareAndSet(s, State.INNER_SUBSCRIBING)) {
                                final var innerPub = err.apply(ex);
                                final var is = new InnerSubscriber<T>(state, this::select);
                                innerSubscriber = is;
                                innerPub.subscribe(is);
                            }
                        }

                        case INNER_SUBSCRIBED -> {
                            if (state.compareAndSet(s, State.INNER_READY)) {
                                final var n = quota.available();
                                if (n > 0) {
                                    innerSubscriber.subscription.request(n);
                                }
                            }
                        }

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

                        case INNER_ERROR -> {
                            final var is = innerSubscriber;
                            this.ex = is.ex;
                            if(state.compareAndSet(s, State.ERROR)) {
                                select();
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

                            /*
                             * 子流完成，跳到指定状态
                             */
                            if (state.compareAndSet(s, State.COMPLETED)) {
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
                    this.ex = ex;
                    if(state.compareAndSet(s, State.ERROR)) {
                        select();
                    }
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
                    quota.requested(n);
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
                    upstreamSubscription.request(n);
                    select();
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
        }

        @Override
        public void onNext(T t) {
            try {
                quota.emitted(1L);
                downstream.onNext(t);
            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            this.ex = ex;
            if (state.compareAndSet(State.SUBSCRIBED, State.MAIN_ERROR)) {
                select();
                return;
            }
            final var ret = state.accumulateAndGet(State.MAIN_ERROR, (s, u) ->
                    switch (s) {
                        case ERROR, COMPLETED, CANCELLED -> s;
                        default -> u;
                    });
            if (ret == State.MAIN_ERROR) {
                select();
            }
        }

        @Override
        public void onComplete() {
            if (state.compareAndSet(State.SUBSCRIBED, State.MAIN_COMPLETED)) {
                select();
                return;
            }
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

    private static class InnerSubscriber<T> implements Flow.Subscriber<T> {

        private final AtomicReference<State> state;
        private final Runnable select;

        private final Queue<T> queue = new ConcurrentLinkedQueue<>();
        private final Quota quota = new Quota();

        private volatile Flow.Subscription subscription;
        private volatile Throwable ex;

        private InnerSubscriber(AtomicReference<State> state, Runnable select) {
            this.state = state;
            this.select = select;
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
        public void onNext(T t) {
            quota.emitted(1L);
            queue.offer(t);
            select.run();
        }

        @Override
        public void onError(Throwable ex) {
            this.ex = ex;
            final var ret = state.accumulateAndGet(State.INNER_ERROR, (s, u) ->
                    switch (s) {
                        case INNER_SUBSCRIBING, INNER_SUBSCRIBED, INNER_READY -> u;
                        default -> s;
                    });
            if (ret == State.INNER_ERROR) {
                select.run();
            }
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
