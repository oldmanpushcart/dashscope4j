package io.github.ompc.dashscope4j.base.api;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;

/**
 * API应答
 *
 * @param <D> 数据类型
 */
public interface ApiResponse<D extends ApiResponse.Output> {

    /**
     * 获取唯一标识
     *
     * @return 唯一标识
     */
    String uuid();

    /**
     * 获取应答结果
     *
     * @return 应答结果
     */
    Ret ret();

    /**
     * 获取用量
     *
     * @return 用量
     */
    Usage usage();

    /**
     * 获取应答数据
     *
     * @return 应答数据
     */
    D output();

    /**
     * 应答数据
     */
    interface Output {
    }

}
