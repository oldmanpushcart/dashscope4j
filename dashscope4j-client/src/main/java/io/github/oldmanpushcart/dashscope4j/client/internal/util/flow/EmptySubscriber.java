package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.concurrent.Flow;

class EmptySubscriber<T> implements Flow.Subscriber<T> {

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    @Override
    public void onNext(T item) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

}
