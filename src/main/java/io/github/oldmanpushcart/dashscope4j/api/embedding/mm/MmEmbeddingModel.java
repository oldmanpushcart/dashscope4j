package io.github.oldmanpushcart.dashscope4j.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 多模态向量计算模型
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class MmEmbeddingModel implements Model {

    /**
     * 向量维度
     */
    int dimension;

    /**
     * 模型名称
     */
    String name;

    /**
     * 远程地址
     */
    URI remote;

    /**
     * MM_EMBEDDING_V1
     * <p>图音文多模态向量计算模型V1版</p>
     */
    public static final MmEmbeddingModel MM_EMBEDDING_V1 = new MmEmbeddingModel(
            1024,
            "multimodal-embedding-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding")
    );

}
