package io.github.oldmanpushcart.internal.dashscope4j.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class JoinFlowPublisher<T> implements Flow.Publisher<T> {

    private volatile Flow.Subscription hold;
    private volatile Flow.Subscriber<? super T> subscriber;
    private final AtomicLong limitRef = new AtomicLong();

    public JoinFlowPublisher(Flow.Publisher<T> publisher, BinaryOperator<T> accumulator, Function<T, CompletableFuture<Flow.Publisher<T>>> finisher) {
        join(publisher, accumulator, finisher);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {

            @Override
            public void request(long n) {
                hold.request(n);
                limitRef.addAndGet(n);
            }

            @Override
            public void cancel() {
                hold.cancel();
            }

        });
    }

    public void join(Flow.Publisher<T> publisher, BinaryOperator<T> accumulator, Function<T, CompletableFuture<Flow.Publisher<T>>> finisher) {
        publisher.subscribe(new Flow.Subscriber<>() {

            private final AtomicReference<T> itemRef = new AtomicReference<>();
            private volatile Flow.Subscription self;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                hold = self = subscription;
            }

            private boolean isSelf() {
                return hold == self;
            }

            private void checkSelf() {
                if (!isSelf()) {
                    throw new IllegalStateException("not self subscription.");
                }
            }

            private void clean() {
                hold = null;
            }

            @Override
            public void onNext(T item) {
                checkSelf();
                limitRef.decrementAndGet();
                try {
                    subscriber.onNext(item);
                    itemRef.accumulateAndGet(item, accumulator);
                } catch (Throwable ex) {
                    onError(ex);
                }
            }

            @Override
            public void onError(Throwable ex) {
                checkSelf();
                clean();
                subscriber.onError(ex);
            }

            @Override
            public void onComplete() {
                checkSelf();
                finisher.apply(itemRef.get()).whenComplete((publisher, ex) -> {

                    // 异常
                    if (null != ex) {
                        onError(ex);
                        return;
                    }

                    // 清理
                    clean();

                    // 续订
                    if (null != publisher) {
                        join(publisher, accumulator, finisher);
                    }

                    // 续订成功，继续请求
                    if (null != hold) {
                        hold.request(limitRef.getAndSet(0L));
                    }

                    // 没有续订，直接完成
                    else {
                        subscriber.onComplete();
                    }

                });

            }

        });

    }

}
