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
     *
     * @since 1.4.2
     */
    String EMPTY_UUID = "";

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
     * @return 是否为最后一个应答
     * @since 1.4.3
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
