package io.github.oldmanpushcart.dashscope4j.embedding.text;

/**
 * 向量计算结果
 */
public interface Embedding extends Comparable<Embedding> {

    /**
     * @return 结果编号
     */
    int index();

    /**
     * @return 向量
     */
    float[] vector();

    @Override
    default int compareTo(Embedding o) {
        return Integer.compare(index(), o.index());
    }

}
