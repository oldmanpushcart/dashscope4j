package io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.Option;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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

    @Getter
    @Accessors(fluent = true)
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    class DefaultMmEmbeddingModel extends BaseModel implements MmEmbeddingModel {

        private final int dimension;

        public DefaultMmEmbeddingModel(int dimension, String name, URI remote, Option option) {
            super(name, remote, option);
            this.dimension = dimension;
        }

        public DefaultMmEmbeddingModel(int dimension, String name, URI remote) {
            super(name, remote);
            this.dimension = dimension;
        }

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
