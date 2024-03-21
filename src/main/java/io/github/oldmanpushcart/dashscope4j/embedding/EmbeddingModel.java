package io.github.oldmanpushcart.dashscope4j.embedding;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;

/**
 * 向量模型
 *
 * @param dimension 向量维度
 * @param name      模型名称
 * @param remote    远程地址
 */
public record EmbeddingModel(int dimension, String name, URI remote) implements Model {

    @Override
    public String label() {
        return "embedding";
    }

    /**
     * TEXT_EMBEDDING_V1
     * <p>文本向量计算模型V1版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语</p>
     */
    public static final EmbeddingModel TEXT_EMBEDDING_V1 = new EmbeddingModel(
            1536,
            "text-embedding-v1",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );

    /**
     * TEXT_EMBEDDING_V2
     * <p>文本向量计算模型V2版</p>
     * <p>中文、英语、西班牙语、法语、葡萄牙语、印尼语、日语、韩语、德语、俄罗斯语</p>
     */
    public static final EmbeddingModel TEXT_EMBEDDING_V2 = new EmbeddingModel(
            1536,
            "text-embedding-v2",
            URI.create("https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding")
    );


}
