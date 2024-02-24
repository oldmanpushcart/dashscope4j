package io.github.ompc.dashscope4j.internal.algo;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

/**
 * 算法应答
 *
 * @param <D> 数据类型
 */
public abstract class AlgoResponse<D extends ApiResponse.Output> extends ApiResponse<D> {
    
    protected AlgoResponse(String uuid, Ret ret, Usage usage, D output) {
        super(uuid, ret, usage, output);
    }

}
