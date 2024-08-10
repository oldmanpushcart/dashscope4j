package io.github.oldmanpushcart.dashscope4j.embedding.text;

import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoResponse;

import java.util.List;

/**
 * 向量计算应答
 */
public interface EmbeddingResponse extends HttpAlgoResponse<EmbeddingResponse.Output> {

    /**
     * 输出
     */
    interface Output {

        /**
         * @return 向量计算结果
         */
        List<Embedding> embeddings();

    }

}
