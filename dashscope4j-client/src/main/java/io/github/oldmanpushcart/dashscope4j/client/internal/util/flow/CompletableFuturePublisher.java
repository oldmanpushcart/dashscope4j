package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

class CompletableFuturePublisher<T> implements Flow.Publisher<T> {

    private final CompletableFuture<? extends Flow.Publisher<T>> future;
    private final Quota quota = new Quota();

    private volatile Flow.Subscription upstream;
    private volatile Flow.Subscriber<? super T> downstream;
    private volatile boolean done;

    public CompletableFuturePublisher(CompletableFuture<? extends Flow.Publisher<T>> future) {
        Objects.requireNonNull(future, "future must not be null!");
        this.future = future;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> d) {
        Objects.requireNonNull(d, "downstream must not be null!");
        synchronized (this) {
            if (downstream != null) {
                d.onSubscribe(new EmptySubscription());
                d.onError(new IllegalStateException("Publisher already subscribed"));
                return;
            }
            downstream = d;
        }

        downstream.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if(upstream == null) {
                    quota.requested(n);
                } else {
                    upstream.request(n);
                }
            }

            @Override
            public void cancel() {
                if(upstream != null) {
                    upstream.cancel();
                }
            }
        });

        subscribeToUpstreamWhenReady(future);
    }

    private void subscribeToUpstreamWhenReady(CompletableFuture<? extends Flow.Publisher<T>> future) {
        future.whenComplete((publisher, ex) -> {
            if (done) {
                return;
            }

            if (null != ex) {
                signalError(ex);
                return;
            }

            try {
                publisher.subscribe(new Flow.Subscriber<>() {

                    @Override
                    public void onSubscribe(Flow.Subscription s) {
                        if (done) {
                            s.cancel();
                            return;
                        }
                        upstream = s;
                        final var n = quota.available();
                        if(n > 0) {
                            s.request(n);
                        }
                    }

                    @Override
                    public void onNext(T t) {
                        if (done) {
                            return;
                        }
                        try {
                            downstream.onNext(t);
                        } catch (Throwable ex) {
                            signalError(ex);
                        }
                    }

                    @Override
                    public void onError(Throwable ex) {
                        signalError(ex);
                    }

                    @Override
                    public void onComplete() {
                        synchronized (this) {
                            if (done) {
                                return;
                            }
                            done = true;
                        }
                        try {
                            downstream.onComplete();
                        } catch (Throwable ignored) {
                        }
                    }

                });
            } catch (Throwable subscribeEx) {
                signalError(subscribeEx);
            }

        });
    }

    private void signalError(Throwable ex) {
        synchronized (this) {
            if (done) {
                return;
            }
            done = true;
        }

        // Cancel upstream stage if available
        future.cancel(true);

        // Cancel upstream if available
        final var u = upstream;
        if (u != null) {
            upstream = null;
            u.cancel();
        }

        // Signal error to downstream
        final var d = downstream;
        if (d != null) {
            try {
                d.onError(ex);
            } catch (Throwable ignored) {
            }
        }
    }

}
