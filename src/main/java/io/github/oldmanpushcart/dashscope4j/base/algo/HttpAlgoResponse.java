package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

/**
 * HTTP类算法应答
 *
 * @param <D> 应答数据类型
 * @since 2.2.0
 */
public interface HttpAlgoResponse<D> extends HttpApiResponse<D>, AlgoResponse<D> {

}
