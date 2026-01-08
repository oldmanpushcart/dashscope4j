package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.concurrent.Flow;

class EmptySubscription implements Flow.Subscription {

    @Override
    public void request(long n) {

    }

    @Override
    public void cancel() {

    }

}
