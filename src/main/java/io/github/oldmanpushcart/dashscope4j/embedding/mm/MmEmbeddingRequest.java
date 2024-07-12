package io.github.oldmanpushcart.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.internal.dashscope4j.embedding.mm.MmEmbeddingRequestBuilderImpl;

import java.util.List;

/**
 * 多模态向量计算请求
 */
public interface MmEmbeddingRequest extends AlgoRequest<MmEmbeddingModel, MmEmbeddingResponse> {

    /**
     * @return 内容列表
     */
    List<Content<?>> contents();

    /**
     * @return 构造器
     */
    static Builder newBuilder() {
        return new MmEmbeddingRequestBuilderImpl();
    }

    /**
     * 构造多模态向量计算请求
     *
     * @param request 请求
     * @return 构造器
     */
    static Builder newBuilder(MmEmbeddingRequest request) {
        return new MmEmbeddingRequestBuilderImpl(request);
    }

    /**
     * 构造器
     */
    interface Builder extends AlgoRequest.Builder<MmEmbeddingModel, MmEmbeddingRequest, Builder> {

        /**
         * 设置文档内容集合
         *
         * @param contents 文档内容集合
         * @return 构造器
         */
        Builder contents(List<Content<?>> contents);

    }

}
