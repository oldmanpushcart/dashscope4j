package io.github.oldmanpushcart.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

/**
 * 多模态向量计算响应
 */
public interface MmEmbeddingResponse extends HttpAlgoResponse<MmEmbeddingResponse.Output>, HttpApiResponse<MmEmbeddingResponse.Output> {

    /**
     * 多模态向量计算响应输出
     */
    interface Output extends HttpAlgoResponse.Output {

        /**
         * @return 多模态向量计算结果
         */
        MmEmbedding embedding();

    }

}
