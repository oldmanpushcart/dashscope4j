package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm.MmEmbeddingRequestBuilderImpl;

import java.util.List;

/**
 * 多模态向量计算请求
 *
 * @since 1.3.0
 */
public interface MmEmbeddingRequest extends SpecifyModelAlgoRequest<MmEmbeddingModel, MmEmbeddingResponse> {

    /**
     * @return 内容列表
     * @since 1.4.0
     */
    List<FactorContent<?>> contents();

    /**
     * @return 构造器
     */
    static Builder newBuilder() {
        return new MmEmbeddingRequestBuilderImpl();
    }

    /**
     * @param request 请求
     * @return 构造器
     * @since 1.4.0
     */
    static Builder newBuilder(MmEmbeddingRequest request) {
        return new MmEmbeddingRequestBuilderImpl(request);
    }

    /**
     * 多模态向量计算请求构造器
     */
    interface Builder extends SpecifyModelAlgoRequest.Builder<MmEmbeddingModel, MmEmbeddingRequest, Builder> {

        /**
         * 添加文档内容
         *
         * @param contents 内容
         * @return 构造器
         */
        Builder contents(FactorContent<?>... contents);

    }

}
