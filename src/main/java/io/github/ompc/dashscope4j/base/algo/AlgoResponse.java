package io.github.ompc.dashscope4j.base.algo;

import io.github.ompc.dashscope4j.base.api.ApiResponse;

/**
 * 算法应答
 *
 * @param <D> 数据类型
 */
public interface AlgoResponse<D extends ApiResponse.Output> extends ApiResponse<D> {

}
