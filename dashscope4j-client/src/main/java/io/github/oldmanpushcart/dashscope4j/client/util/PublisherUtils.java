package io.github.oldmanpushcart.dashscope4j.client.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletionStage;

/**
 * 响应式发布器工具类。
 * <p>
 * 提供将 {@link CompletionStage} (异步计算结果) 安全转换为 Reactor {@link Publisher} 的工具方法。
 * 核心特性是支持<b>取消信号的联动传播</b>：当上游的 Flux 被取消订阅时，底层的异步任务也会被主动取消，
 * 从而避免不必要的资源消耗或网络请求。
 */
public class PublisherUtils {

    /**
     * 将一个返回 {@link Publisher} 的 {@link CompletionStage} 转换为扁平化的 {@link Publisher}。
     * <p>
     * 适用于异步操作的结果本身也是一个响应式流的场景（例如：异步获取 SSE 流、异步获取分页数据流等）。
     * 该方法会将外层的 Future 和内层的 Publisher 进行无缝衔接与扁平化。
     *
     * @param stage 异步阶段，其完成后的结果是一个 Publisher
     * @param <R>   最终发布的元素类型
     * @return 扁平化后的 Publisher，直接发射 R 类型的元素
     */
    public static <R> Publisher<R> fromCancellableStage(CompletionStage<? extends Publisher<R>> stage) {
        return Mono.fromFuture(stage.toCompletableFuture(), true)
                .flatMapMany(p -> p);
    }

    /**
     * 将一个普通的 {@link CompletionStage} 转换为 {@link Publisher}。
     * <p>
     * 适用于单次异步计算结果的响应式包装（例如：异步获取配置、异步鉴权等）。
     * 该 Publisher 最多只会发射一个元素，随后即完成。
     *
     * @param stage 异步阶段，其完成后的结果是单个对象
     * @param <R>   发布的元素类型
     * @return 包装后的 Publisher
     */
    public static <R> Publisher<R> unwrapCancellableStage(CompletionStage<? extends R> stage) {
        return Mono.fromFuture(stage.toCompletableFuture(), true);
    }

}