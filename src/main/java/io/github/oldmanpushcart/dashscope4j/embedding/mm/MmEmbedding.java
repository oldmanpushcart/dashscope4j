package io.github.oldmanpushcart.dashscope4j.embedding.mm;

/**
 * 多模态向量计算结果
 */
public interface MmEmbedding {

    /**
     * @return 向量
     */
    float[] vector();

}
