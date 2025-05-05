package io.github.oldmanpushcart.dashscope4j.client.api.embedding.text;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
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

    @Getter
    @Accessors(fluent = true)
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class BaseEmbeddingModel extends BaseModel implements EmbeddingModel {

        private final int dimension;

        public BaseEmbeddingModel(int dimension, String name, URI remote, Option option) {
            super(name, remote, option);
            this.dimension = dimension;
        }

        public BaseEmbeddingModel(int dimension, String name, URI remote) {
            super(name, remote);
            this.dimension = dimension;
        }

    }

    /**
     * TEXT_EMBEDDING_V1
     * <p>文本向量计算模型V1版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V1 = new BaseEmbeddingModel(
            1536,
            "text-embedding-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

    /**
     * TEXT_EMBEDDING_V2
     * <p>文本向量计算模型V2版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语、日语、韩语、德语、俄罗斯语</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V2 = new BaseEmbeddingModel(
            1536,
            "text-embedding-v2",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

    /**
     * TEXT_EMBEDDING_V3
     * <p>文本向量计算模型V3版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语、日语、韩语、德语、俄语等50+语种</p>
     */
    EmbeddingModel TEXT_EMBEDDING_V3 = new BaseEmbeddingModel(
            1024,
            "text-embedding-v3",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

}
