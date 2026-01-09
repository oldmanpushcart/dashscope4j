package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class IterablePublisher<T> implements Flow.Publisher<T> {

    private final Iterable<T> iterable;

    public IterablePublisher(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable must not be null!");
        this.iterable = iterable;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber must not be null!");
        try {
            final var iterator = iterable.iterator();
            subscriber.onSubscribe(new IterableSubscription<>(subscriber, iterator));
        } catch (Throwable ex) {
            subscriber.onSubscribe(new EmptySubscription());
            subscriber.onError(ex);
        }
    }

    private static class IterableSubscription<T> implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<T> iterator;
        private final Quota quota = new Quota();
        private final AtomicBoolean done = new AtomicBoolean();

        private IterableSubscription(Flow.Subscriber<? super T> subscriber, Iterator<T> iterator) {
            this.subscriber = subscriber;
            this.iterator = iterator;
        }

        @Override
        public void request(long n) {
            try {
                if (n <= 0) {
                    throw new IllegalArgumentException("n must be positive!");
                }
                if(done.get()) {
                    return;
                }

                quota.requested(n);
                while(quota.available() > 0 && !done.get()) {
                    if(iterator.hasNext()) {
                        final var item = iterator.next();
                        quota.emitted(1);
                        subscriber.onNext(item);
                    } else {
                        if(done.compareAndSet(false, true)) {
                            subscriber.onComplete();
                            return;
                        }
                    }
                }

            } catch (Throwable ex) {
                if (done.compareAndSet(false, true)) {
                    subscriber.onError(ex);
                }
            }

        }

        @Override
        public void cancel() {
            done.set(true);
        }

    }

}
