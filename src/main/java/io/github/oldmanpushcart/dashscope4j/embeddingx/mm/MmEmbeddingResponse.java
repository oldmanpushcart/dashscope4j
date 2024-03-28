package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoResponse;

/**
 * 多模态向量计算响应
 *
 * @since 1.3.0
 */
public interface MmEmbeddingResponse extends AlgoResponse<MmEmbeddingResponse.Output> {

    /**
     * 多模态向量计算响应输出
     */
    interface Output extends AlgoResponse.Output {

        /**
         * @return 多模态向量计算结果
         */
        MmEmbedding embedding();

    }

}
