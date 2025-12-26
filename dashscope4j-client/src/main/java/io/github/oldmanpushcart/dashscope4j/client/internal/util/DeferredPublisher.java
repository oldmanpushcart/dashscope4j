package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class DeferredPublisher<T> implements Flow.Publisher<T> {

    private final CompletionStage<? extends Flow.Publisher<T>> publisherStage;

    public DeferredPublisher(CompletionStage<? extends Flow.Publisher<T>> publisherStage) {
        this.publisherStage = publisherStage;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        // 使用一个中介订阅者来桥接
        var bridge = new BridgeSubscriber<>(subscriber, publisherStage);
        subscriber.onSubscribe(bridge);
    }

    // 内部桥接订阅者
    private static class BridgeSubscriber<T> implements Flow.Subscription {
        private final Flow.Subscriber<? super T> downstream;
        private final CompletionStage<? extends Flow.Publisher<T>> publisherStage;
        private volatile Flow.Subscription upstream;
        private volatile boolean cancelled = false;

        BridgeSubscriber(Flow.Subscriber<? super T> downstream,
                         CompletionStage<? extends Flow.Publisher<T>> publisherStage) {
            this.downstream = downstream;
            this.publisherStage = publisherStage;
        }

        @Override
        public void request(long n) {
            var u = upstream;
            if (u != null) {
                u.request(n);
            } else {
                // 如果上游还没就绪，先保存请求（简单处理：暂存到 future 完成后）
                // 更健壮的做法是用队列，但这里假设 request 在 onSubscribe 后调用
                publisherStage.thenAccept(publisher -> {
                    publisher.subscribe(new Flow.Subscriber<T>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {
                            upstream = subscription;
                            if (!cancelled) {
                                subscription.request(n);
                            } else {
                                subscription.cancel();
                            }
                        }

                        @Override
                        public void onNext(T item) {
                            if (!cancelled) downstream.onNext(item);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            if (!cancelled) downstream.onError(throwable);
                        }

                        @Override
                        public void onComplete() {
                            if (!cancelled) downstream.onComplete();
                        }
                    });
                }).exceptionally(ex -> {
                    if (!cancelled) downstream.onError(ex);
                    return null;
                });
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            var u = upstream;
            if (u != null) {
                u.cancel();
            }
        }
    }
}