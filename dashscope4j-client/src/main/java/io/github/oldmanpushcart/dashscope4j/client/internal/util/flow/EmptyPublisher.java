package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.concurrent.Flow;

public class EmptyPublisher<T> implements Flow.Publisher<T> {

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        });
        subscriber.onComplete();
    }

    public static <T> EmptyPublisher<T> empty() {
        return new EmptyPublisher<>();
    }

}
