package io.github.oldmanpushcart.dashscope4j;

/**
 * 异步&流式&任务操作
 *
 * @param <R> 结果类型
 */
public interface OpAsyncOpFlowOpTask<R> extends OpAsync<R>, OpFlow<R>, OpTask<R> {

}
