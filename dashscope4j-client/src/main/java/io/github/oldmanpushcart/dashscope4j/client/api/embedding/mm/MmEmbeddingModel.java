package io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 多模态向量计算模型
 */
public interface MmEmbeddingModel extends Model {

    /**
     * 向量维度
     */
    int dimension();

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class DefaultMmEmbeddingModel implements MmEmbeddingModel {
        int dimension;
        String name;
        URI remote;
    }

    /**
     * MM_EMBEDDING_V1
     * <p>图音文多模态向量计算模型V1版</p>
     */
    MmEmbeddingModel MM_EMBEDDING_V1 = new DefaultMmEmbeddingModel(
            1024,
            "multimodal-embedding-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding")
    );

}
