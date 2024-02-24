package io.github.ompc.dashscope4j.api;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;

/**
 * API响应
 *
 * @param <D> 数据类型
 */
public interface ApiResponse<D extends ApiResponse.Output> {

    String uuid();

    Ret ret();

    Usage usage();

    D output();


    /**
     * 应答数据
     */
    interface Output {
    }

}
