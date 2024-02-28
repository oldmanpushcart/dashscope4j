package io.github.oldmanpushcart.dashscope4j.embedding;

/**
 * 向量计算结果
 */
public interface Embedding extends Comparable<Embedding> {

    /**
     * 获取结果编号
     * @return 结果编号
     */
    int index();

    /**
     * 获取向量
     * @return 向量
     */
    float[] vector();

    @Override
    default int compareTo(Embedding o) {
        return Integer.compare(index(), o.index());
    }

}
