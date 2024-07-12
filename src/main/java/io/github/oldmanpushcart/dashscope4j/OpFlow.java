package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * 流式操作
 *
 * @param <R> 结果类型
 */
public interface OpFlow<R> {

    /**
     * @return 流式操作
     */
    CompletableFuture<Flow.Publisher<R>> flow();

    /**
     * 流式操作
     *
     * @param subscriber 订阅者
     * @return 操作结果
     */
    default CompletableFuture<Void> flow(Flow.Subscriber<R> subscriber) {
        return flow().thenAccept(publisher -> publisher.subscribe(subscriber));
    }

}
