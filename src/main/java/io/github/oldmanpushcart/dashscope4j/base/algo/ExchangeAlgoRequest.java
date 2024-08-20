package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiRequest;

/**
 * 数据交互类算法请求
 *
 * @param <M> 模型类型
 * @param <R> 应答类型
 * @since 2.2.0
 */
public interface ExchangeAlgoRequest<M extends Model, R extends ExchangeAlgoResponse<?>>
        extends ExchangeApiRequest<R>, AlgoRequest<M> {

}
