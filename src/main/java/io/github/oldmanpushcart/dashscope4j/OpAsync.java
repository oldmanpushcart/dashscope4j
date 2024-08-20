package io.github.oldmanpushcart.dashscope4j;

import java.util.concurrent.CompletionStage;

/**
 * 异步操作
 *
 * @param <R> 结果类型
 */
public interface OpAsync<R> {

    /**
     * @return 异步操作
     */
    CompletionStage<R> async();

}
