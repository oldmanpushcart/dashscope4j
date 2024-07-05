package io.github.oldmanpushcart.dashscope4j.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 消费流订阅者
 *
 * @param <T> 元素类型
 */
public class ConsumeFlowSubscriber<T> implements Flow.Subscriber<T> {

    private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();
    private final Consumer<T> consumer;
    private final CompletableFuture<Void> completed = new CompletableFuture<>();

    /**
     * 构造消费流订阅者
     *
     * @param consumer 消费者
     */
    public ConsumeFlowSubscriber(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (!subscriptionRef.compareAndSet(null, subscription)) {
            subscription.cancel();
            throw new IllegalStateException("already subscribed");
        }
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        try {
            consumer.accept(item);
            subscriptionRef.get().request(1);
        } catch (Throwable t) {
            onError(t);
        }
    }

    @Override
    public void onError(Throwable ex) {
        subscriptionRef.get().cancel();
        completed.completeExceptionally(ex);
    }

    @Override
    public void onComplete() {
        completed.complete(null);
    }

    /**
     * 获取完成通知
     *
     * @return 完成通知
     */
    public CompletableFuture<Void> completed() {
        return completed;
    }

    /**
     * 消费流
     *
     * @param publisher 发布者
     * @param consumer  消费者
     * @param <T>       元素类型
     */
    public static <T> void consume(Flow.Publisher<T> publisher, Consumer<T> consumer) {
        publisher.subscribe(new ConsumeFlowSubscriber<>(consumer));
    }

    /**
     * 消费流
     *
     * @param publisher 发布者
     * @param consumer  消费者
     * @param <T>       元素类型
     * @return 消费流订阅者
     */
    public static <T> CompletableFuture<Void> consumeCompose(Flow.Publisher<T> publisher, Consumer<T> consumer) {
        final var subscriber = new ConsumeFlowSubscriber<>(consumer);
        publisher.subscribe(subscriber);
        return subscriber.completed();
    }

}
