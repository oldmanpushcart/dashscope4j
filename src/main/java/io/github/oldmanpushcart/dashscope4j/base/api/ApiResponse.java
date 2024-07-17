package io.github.oldmanpushcart.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;

/**
 * API应答
 *
 * @param <D> 数据类型
 */
public interface ApiResponse<D extends ApiResponse.Output> {

    /**
     * 空UUID
     */
    String EMPTY_UUID = "";

    /**
     * @return 唯一标识
     */
    String uuid();

    /**
     * @return 应答结果
     */
    Ret ret();

    /**
     * @return 用量
     */
    Usage usage();

    /**
     * @return 应答数据
     */
    D output();

    /**
     * @return 是否为最后一个应答
     */
    default boolean isLast() {
        return true;
    }

    /**
     * 应答数据
     */
    interface Output {
    }

}
