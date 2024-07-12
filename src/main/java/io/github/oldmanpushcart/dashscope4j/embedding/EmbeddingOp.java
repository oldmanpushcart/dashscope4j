package io.github.oldmanpushcart.dashscope4j.embedding;

import io.github.oldmanpushcart.dashscope4j.OpAsync;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingResponse;

/**
 * 向量计算操作
 */
public interface EmbeddingOp {

    /**
     * 文本向量计算
     *
     * @param request 向量计算请求
     * @return 操作
     */
    OpAsync<EmbeddingResponse> text(EmbeddingRequest request);

    /**
     * 多模态向量计算
     *
     * @param request 多模态向量计算请求
     * @return 操作
     */
    OpAsync<MmEmbeddingResponse> mm(MmEmbeddingRequest request);

}
