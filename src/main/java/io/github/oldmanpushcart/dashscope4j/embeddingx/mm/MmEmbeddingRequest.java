package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm.MmEmbeddingRequestBuilderImpl;

/**
 * 多模态向量计算请求
 *
 * @since 1.3.0
 */
public interface MmEmbeddingRequest extends AlgoRequest<MmEmbeddingResponse> {

    /**
     * @return 构造器
     */
    static Builder newBuilder() {
        return new MmEmbeddingRequestBuilderImpl();
    }

    /**
     * 多模态向量计算请求构造器
     */
    interface Builder extends AlgoRequest.Builder<MmEmbeddingModel, MmEmbeddingRequest, Builder> {

        /**
         * 添加文档内容
         *
         * @param contents 内容
         * @return 构造器
         */
        Builder contents(FactorContent<?>... contents);

    }

}
