package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletableFuture;

/**
 * 异步操作
 *
 * @param <R> 结果类型
 */
public interface OpAsync<R> {

    /**
     * @return 异步操作
     */
    CompletableFuture<R> async();

}
