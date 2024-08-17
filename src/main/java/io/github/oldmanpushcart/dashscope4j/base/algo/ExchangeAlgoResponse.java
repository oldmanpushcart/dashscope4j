package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiResponse;

/**
 * 数据交互类算法应答
 *
 * @param <D> 应答数据类型
 * @since 2.2.0
 */
public interface ExchangeAlgoResponse<D> extends ExchangeApiResponse<D>, AlgoResponse<D> {

}
