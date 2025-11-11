package io.github.oldmanpushcart.dashscope4j.client;

import java.util.concurrent.Flow;

/**
 * 流式操作
 *
 * @param <T> 请求类型
 * @param <R> 应答类型
 */
public interface OpFlow<T, R> {

    /**
     * 流式操作
     *
     * @param request 请求
     * @return 流式应答
     */
    Flow.Publisher<R> flow(T request);

}
