package io.github.ompc.dashscope4j.internal.algo;

import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiData;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

/**
 * 算法应答
 *
 * @param <D> 数据类型
 */
public abstract class AlgoResponse<D extends ApiData> extends ApiResponse<D> {
    
    protected AlgoResponse(String uuid, String code, String message, Usage usage, D data) {
        super(uuid, code, message, usage, data);
    }

}
