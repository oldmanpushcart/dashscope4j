package io.github.oldmanpushcart.dashscope4j.embeddingx.mm;

/**
 * 多模态向量计算结果
 *
 * @since 1.3.0
 */
public interface MmEmbedding {

    /**
     * @return 向量
     */
    float[] vector();

}
