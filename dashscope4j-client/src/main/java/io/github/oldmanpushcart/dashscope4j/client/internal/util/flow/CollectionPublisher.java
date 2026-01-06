package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CollectionPublisher<T> implements Flow.Publisher<T> {

    private final Collection<T> collection;

    public CollectionPublisher(Collection<T> collection) {
        if (collection == null) {
            throw new NullPointerException("collection must not be null");
        }
        this.collection = collection;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("subscriber must not be null");
        }
        // Create iterator upfront — assumes collection is not modified during emission
        Iterator<T> it;
        try {
            it = collection.iterator();
        } catch (Throwable t) {
            // Fail fast if iterator() throws
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                }
            });
            subscriber.onError(t);
            return;
        }

        subscriber.onSubscribe(new CollectionSubscription<>(subscriber, it));
    }

    // ==============================
    // Subscription Implementation
    // ==============================
    private static final class CollectionSubscription<T> implements Flow.Subscription {

        private final Flow.Subscriber<? super T> downstream;
        private final Iterator<T> iterator;

        private final AtomicLong requested = new AtomicLong();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        CollectionSubscription(Flow.Subscriber<? super T> downstream, Iterator<T> iterator) {
            this.downstream = downstream;
            this.iterator = iterator;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                cancel();
                downstream.onError(new IllegalArgumentException("non-positive request: " + n));
                return;
            }

            if (cancelled.get()) {
                return;
            }

            // Add demand
            for (; ; ) {
                long r = requested.get();
                if (r == Long.MAX_VALUE) return;
                long u = r + n;
                if (u < 0L) u = Long.MAX_VALUE; // overflow guard
                if (requested.compareAndSet(r, u)) {
                    break;
                }
            }

            drain();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        private void drain() {
            if (cancelled.get()) {
                return;
            }

            long emitted = 0;
            long r = requested.get();

            // Emit up to min(available demand, available items)
            while (emitted < r && !cancelled.get()) {
                T item;
                boolean hasNext;
                try {
                    hasNext = iterator.hasNext();
                } catch (Throwable t) {
                    cancel();
                    downstream.onError(t);
                    return;
                }

                if (!hasNext) {
                    // No more items → complete
                    if (!cancelled.get()) {
                        downstream.onComplete();
                    }
                    return;
                }

                try {
                    item = iterator.next();
                } catch (Throwable t) {
                    cancel();
                    downstream.onError(t);
                    return;
                }

                if (item == null) {
                    cancel();
                    downstream.onError(new NullPointerException("Collection contains null element"));
                    return;
                }

                downstream.onNext(item);
                emitted++;
            }

            // Reduce the requested amount by what we emitted
            if (emitted > 0) {
                requested.addAndGet(-emitted);
            }
        }
    }

    @SafeVarargs
    public static <T> Flow.Publisher<T> of(T... items) {
        return new CollectionPublisher<>(List.of(items));
    }

    public static <T> Flow.Publisher<T> of(Collection<T> collection) {
        return new CollectionPublisher<>(collection);
    }

}
