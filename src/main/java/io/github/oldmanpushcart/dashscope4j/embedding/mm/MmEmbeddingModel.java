package io.github.oldmanpushcart.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;

/**
 * 多模态向量计算模型
 *
 * @param dimension 向量维度
 * @param name      模型名称
 * @param remote    远程地址
 */
public record MmEmbeddingModel(int dimension, String name, URI remote) implements Model {

    @Override
    public String label() {
        return "embeddingx";
    }

    /**
     * MM_EMBEDDING_ONE_PEACE_V1
     * <p>图音文多模态向量计算模型V1版</p>
     */
    public static final MmEmbeddingModel MM_EMBEDDING_ONE_PEACE_V1 = new MmEmbeddingModel(
            1536,
            "multimodal-embedding-one-peace-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding")
    );

}
