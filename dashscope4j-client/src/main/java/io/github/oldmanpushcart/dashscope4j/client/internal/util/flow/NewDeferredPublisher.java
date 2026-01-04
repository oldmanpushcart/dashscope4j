package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class NewDeferredPublisher <T> implements Flow.Publisher<T> {

    private final Supplier<CompletableFuture<? extends Flow.Publisher<T>>> upstreamFutureSupplier;

    public NewDeferredPublisher(Supplier<CompletableFuture<? extends Flow.Publisher<T>>> upstreamFutureSupplier) {
        this.upstreamFutureSupplier = upstreamFutureSupplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {

    }

}
