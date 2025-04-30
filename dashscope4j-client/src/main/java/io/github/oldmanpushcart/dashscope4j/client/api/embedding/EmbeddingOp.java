package io.github.oldmanpushcart.dashscope4j.client.api.embedding;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.text.EmbeddingResponse;

/**
 * 向量计算操作
 */
public interface EmbeddingOp {

    /**
     * @return 文本向量计算
     */
    OpAsync<EmbeddingRequest, EmbeddingResponse> text();

    /**
     * @return 多模态向量计算
     */
    OpAsync<MmEmbeddingRequest, MmEmbeddingResponse> mm();

}
