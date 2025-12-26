package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

public class DeferredPublisher<T> implements Flow.Publisher<T> {

    private final CompletionStage<? extends Flow.Publisher<T>> upstreamStage;
    private final Object lock = new Object();
    private final AtomicLong requestedRef = new AtomicLong(0L);

    private volatile Flow.Subscriber<? super T> downstream;
    private volatile Flow.Subscription upstream;
    private volatile boolean done;

    public DeferredPublisher(CompletionStage<? extends Flow.Publisher<T>> upstreamStage) {
        this.upstreamStage = upstreamStage;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> downstream) {

        Objects.requireNonNull(downstream);

        /*
         * 检查否已经订阅
         */
        if (this.downstream != null) {
            downstream.onSubscribe(new EmptySubscription());
            downstream.onError(new IllegalStateException("Publisher already subscribed"));
            return;
        }

        this.downstream = downstream;
        downstream.onSubscribe(new Flow.Subscription() {

            @Override
            public void request(long n) {

                if (n <= 0) {
                    signalError(new IllegalArgumentException("non-positive request: " + n));
                    return;
                }

                var u = upstream;

                if (null != u) {
                    u.request(n);
                } else {
                    synchronized (lock) {
                        u = upstream;
                        if (null != u) {
                            u.request(n);
                        } else if (!done) {
                            requestedRef.accumulateAndGet(n, (requested, delta) -> {
                                final var result = requested + delta;
                                return result < 0 ? Long.MAX_VALUE : result;
                            });
                        }
                    }
                }

            }

            @Override
            public void cancel() {
                if (done) {
                    return;
                }
                done = true;

                final var u = upstream;
                if (null != u) {
                    upstream = null;
                    u.cancel();
                }

            }

        });

        subscribeToUpstreamWhenReady(upstreamStage);

    }

    private void subscribeToUpstreamWhenReady(CompletionStage<? extends Flow.Publisher<T>> stage) {
        stage.whenComplete((publisher, ex) -> {

            if (done) {
                return;
            }

            if (null != ex) {
                signalError(ex);
                return;
            }

            if (null == publisher) {
                signalError(new NullPointerException("Publisher from CompletionStage is null"));
                return;
            }

            publisher.subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription upstream) {
                    if (done) {
                        upstream.cancel();
                        return;
                    }
                    long r;
                    synchronized (lock) {
                        if (done) {
                            upstream.cancel();
                            return;
                        }
                        DeferredPublisher.this.upstream = upstream;
                        r = requestedRef.getAndSet(0L);
                    }
                    if (r > 0) {
                        upstream.request(r);
                    }
                }

                @Override
                public void onNext(T item) {
                    if (!done) {
                        DeferredPublisher.this.downstream.onNext(item);
                    }
                }

                @Override
                public void onError(Throwable ex) {
                    if (!done) {
                        signalError(ex);
                    }
                }

                @Override
                public void onComplete() {
                    if (!done) {
                        done = true;
                        try {
                            DeferredPublisher.this.downstream.onComplete();
                        } catch (Throwable ignored) {
                            // ignore
                        }
                    }
                }

            });
        });
    }

    private void signalError(Throwable ex) {
        if (!done) {
            done = true;

            // Cancel upstream if available
            Flow.Subscription u = upstream;
            if (u != null) {
                u.cancel();
            }

            // Signal error to downstream
            Flow.Subscriber<? super T> d = downstream;
            if (d != null) {
                try {
                    d.onError(ex);
                } catch (Throwable ignored) {
                }
            }
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

}