package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class DebugDemoTestCase {

    @Test
    public void debug() throws InterruptedException {

        final var latch = new CountDownLatch(1);
        final var publisher = new SubmissionPublisher<String>();
        final var publisher2 = new DeferredPublisher<>(() -> {
            final var executor = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);
            return CompletableFuture.supplyAsync(()-> publisher, executor);
        });
        final var publisher3 = new DeferredPublisher<>(() -> {
            final var executor = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);
            return CompletableFuture.supplyAsync(()-> publisher2, executor);
        });

        publisher3.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                System.out.println("subscribed");
            }

            @Override
            public void onNext(String item) {

            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("completed");
                latch.countDown();
            }

        });

        publisher.closeExceptionally(new RuntimeException("TEST!"));
        latch.await();

    }

}
