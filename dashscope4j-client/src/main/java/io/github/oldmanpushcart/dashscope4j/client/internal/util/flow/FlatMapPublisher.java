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

    public FlatMapPublisher(Flow.Publisher<T> upstream, Function<T, Flow.Publisher<R>> each, Supplier<Flow.Publisher<R>> fin) {
        this.upstream = upstream;
        this.each = each;
        this.fin = fin;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> downstream) {
        Objects.requireNonNull(downstream);
        upstream.subscribe(new UpstreamSubscriber<>(downstream, each, fin));
    }

    private enum State {
        INIT,
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

        public long requested() {
            return requested.get();
        }

        public void requested(long delta) {
            safetyAccumulateAndGet(requested, delta);
        }

        public long emitted() {
            return emitted.get();
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
        private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
        private final Quota quota = new Quota();
        private final Quota upstreamQuota = new Quota();
        private final Queue<Flow.Publisher<R>> queue = new ConcurrentLinkedQueue<>();
        private final Queue<R> rQueue = new ConcurrentLinkedQueue<>();

        private volatile Throwable ex;
        private volatile boolean upstreamCompleted;
        private volatile Flow.Subscription upstreamSubscription;
        private volatile InnerSubscriber<R> innerSubscriber;

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

                final var s = state.get();
                switch (s) {

                    case SUBSCRIBED -> {
                        if (state.compareAndSet(s, State.READY)) {
                            upstreamSubscription.request(1L);
                        }
                    }

                    case READY -> {
                        if (!queue.isEmpty()) {
                            if (state.compareAndSet(s, State.INNER_SUBSCRIBING)) {
                                final var innerPub = Objects.requireNonNull(queue.poll());
                                final var is = new InnerSubscriber<R>(state, upstreamCompleted, this::select, this::onError);
                                innerSubscriber = is;
                                innerPub.subscribe(is);
                            }
                            break;
                        }
                        if (upstreamCompleted) {
                            final var innerPub = Objects.requireNonNull(fin.get());
                            queue.offer(innerPub);
                            select();
                            break;
                        }
                        if (upstreamQuota.available() == 0) {
                            upstreamSubscription.request(1L);
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
                        if (!is.queue.isEmpty()) {
                            final var r = Objects.requireNonNull(is.queue.poll());
                            downstream.onNext(r);
                            select();
                            break;
                        }
                        if (is.done && is.terminal) {
                            if (state.compareAndSet(s, State.COMPLETED)) {
                                select();
                                break;
                            }
                        }
                        if (is.done && !is.terminal) {
                            if (state.compareAndSet(s, State.READY)) {
                                select();
                                break;
                            }
                        }
                        if (is.quota.available() == 0) {
                            final var n = quota.available();
                            if (n > 0) {
                                is.subscription.request(n);
                            }
                        }
                    }

                }

                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {

            if (!state.compareAndSet(State.INIT, State.SUBSCRIBED)) {
                s.cancel();
                downstream.onSubscribe(new EmptySubscription());
                downstream.onError(new IllegalStateException("Publisher already subscribed"));
            }

            downstream.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        onError(new IllegalArgumentException("n must be positive"));
                        return;
                    }
                    quota.requested(n);
                    upstreamQuota.requested(n);
                    select();
                }

                @Override
                public void cancel() {
                    state.set(State.CANCELLED);
                    select();
                }
            });
            upstreamSubscription = s;
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
            upstreamQuota.emitted(1L);
            upstreamCompleted = true;
            select();
        }

    }

    private static class InnerSubscriber<R> implements Flow.Subscriber<R> {

        private final AtomicReference<State> state;
        private final boolean terminal;
        private final Runnable select;
        private final Consumer<Throwable> onError;
        private final Queue<R> queue = new ConcurrentLinkedQueue<>();
        private final Quota quota = new Quota();
        private volatile Flow.Subscription subscription;
        private volatile boolean done;

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
            done = true;
            select.run();
        }

    }

}
