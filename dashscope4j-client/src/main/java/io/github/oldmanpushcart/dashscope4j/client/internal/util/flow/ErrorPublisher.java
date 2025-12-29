package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.Objects;
import java.util.concurrent.Flow;

public class ErrorPublisher<T> implements Flow.Publisher<T> {

    private final Throwable cause;

    public ErrorPublisher(Throwable cause) {
        this.cause = Objects.requireNonNull(cause);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        subscriber.onSubscribe(new EmptySubscription());
        subscriber.onError(cause);
    }

    private static final class EmptySubscription implements Flow.Subscription {

        @Override
        public void request(long n) {

        }

        @Override
        public void cancel() {

        }

    }

}
