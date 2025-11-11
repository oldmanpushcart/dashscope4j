package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.concurrent.Flow;
import java.util.function.Function;

public class MapPublisher<T, R> implements Flow.Publisher<R> {

    private final Flow.Publisher<T> upstream;
    private final Function<T, R> mapper;

    public MapPublisher(Flow.Publisher<T> upstream, Function<T, R> mapper) {
        this.upstream = upstream;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        upstream.subscribe(new MappingSubscriber(subscriber));
    }

    private class MappingSubscriber implements Flow.Subscriber<T> {

        private final Flow.Subscriber<? super R> downstream;

        private MappingSubscriber(Flow.Subscriber<? super R> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            downstream.onSubscribe(new Flow.Subscription() {

                @Override
                public void request(long n) {
                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                }

            });
        }

        @Override
        public void onNext(T item) {
            try {
                downstream.onNext(mapper.apply(item));
            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            downstream.onError(ex);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

    }

}
