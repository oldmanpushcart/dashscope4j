package io.github.oldmanpushcart.dashscope4j.api.embedding.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * 向量计算结果
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class Embedding implements Comparable<Embedding> {

    /**
     * 结果编号
     */
    int index;

    /**
     * 向量矩阵
     */
    @ToString.Exclude
    float[] vector;

    @JsonCreator
    private Embedding(

            @JsonProperty("text_index")
            int index,

            @JsonProperty("embedding")
            float[] vector

    ) {
        this.index = index;
        this.vector = vector;
    }

    @Override
    public int compareTo(@NotNull Embedding o) {
        return Integer.compare(index(), o.index());
    }

}
