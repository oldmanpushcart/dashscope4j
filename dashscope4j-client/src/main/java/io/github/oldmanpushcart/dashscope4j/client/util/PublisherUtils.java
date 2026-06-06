package io.github.oldmanpushcart.dashscope4j.client.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletionStage;

public class PublisherUtils {

    public static <R> Publisher<R> fromCancellableStage(CompletionStage<? extends Publisher<R>> stage) {
        return Flux.<Publisher<R>>create(sink -> {
                    // 1. 监听 CompletionStage 的完成状态
                    stage.whenComplete((publisher, throwable) -> {
                        if (throwable != null) {
                            sink.error(throwable);
                        } else if (publisher != null) {
                            sink.next(publisher);
                            sink.complete();
                        } else {
                            sink.complete();
                        }
                    });

                    // 2. 绑定取消信号：当 Flux 被取消时，联动取消底层的 CompletionStage
                    sink.onDispose(() -> {
                        if (!stage.toCompletableFuture().isDone()) {
                            stage.toCompletableFuture().cancel(true);
                        }
                    });
                })
                // 3. 将外层 Publisher 扁平化为真正的 Flux<R>
                .flatMap(publisher -> publisher);
    }

    public static <R> Publisher<R> unwrapCancellableStage(CompletionStage<? extends R> stage) {
        return Flux.create(sink -> {

            // 1. 监听 CompletionStage 的完成状态
            stage.whenComplete((r, throwable) -> {
                if (throwable != null) {
                    sink.error(throwable);
                } else if (r != null) {
                    sink.next(r);
                    sink.complete();
                } else {
                    sink.complete();
                }
            });

            // 2. 绑定取消信号：当 Flux 被取消时，联动取消底层的 CompletionStage
            sink.onDispose(() -> {
                if (!stage.toCompletableFuture().isDone()) {
                    stage.toCompletableFuture().cancel(true);
                }
            });

        });
    }

}
