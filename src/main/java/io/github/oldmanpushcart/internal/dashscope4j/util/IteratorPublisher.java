package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.Iterator;
import java.util.concurrent.Flow;

public class IteratorPublisher<T> implements Flow.Publisher<T> {

    private final Iterator<T> iterator;

    public IteratorPublisher(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {

            private volatile boolean cancelled = false;

            @Override
            public void request(long n) {
                if (n <= 0) {
                    throw new IllegalArgumentException("n must be positive!");
                }
                while (n-- > 0) {
                    if (cancelled || !iterator.hasNext()) {
                        return;
                    }
                    try {
                        subscriber.onNext(iterator.next());
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }
            }

            @Override
            public void cancel() {
                this.cancelled = true;
            }

        });
    }

    public static <T> IteratorPublisher<T> of(Iterable<T> iterable) {
        return new IteratorPublisher<>(iterable.iterator());
    }

}
