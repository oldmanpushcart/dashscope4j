package io.github.oldmanpushcart.dashscope4j.client.api.embedding.text;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * 文本向量计算模型
 */
public interface EmbeddingModel extends Model {

    /**
     * 向量维度
     */
    int dimension();

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class DefaultEmbeddingModel implements EmbeddingModel {
        int dimension;
        String name;
        URI remote;
    }

    /**
     * TEXT_EMBEDDING_V1
     * <p>文本向量计算模型V1版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V1 = new DefaultEmbeddingModel(
            1536,
            "text-embedding-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

    /**
     * TEXT_EMBEDDING_V2
     * <p>文本向量计算模型V2版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语、日语、韩语、德语、俄罗斯语</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V2 = new DefaultEmbeddingModel(
            1536,
            "text-embedding-v2",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

    /**
     * TEXT_EMBEDDING_V3
     * <p>文本向量计算模型V3版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语、日语、韩语、德语、俄语等50+语种</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V3 = new DefaultEmbeddingModel(
            1024,
            "text-embedding-v3",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

}
