package io.github.oldmanpushcart.dashscope4j.api.embedding.mm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * 多模态向量计算结果
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class MmEmbedding {

    /**
     * 结果编号
     */
    int index;

    /**
     * 结果类型
     */
    Content.Type type;

    /**
     * 向量矩阵
     */
    @ToString.Exclude
    float[] vector;

    @JsonCreator
    private MmEmbedding(

            @JsonProperty("index")
            int index,

            @JsonProperty("embedding")
            float[] vector,

            @JsonProperty("type")
            Content.Type type

    ) {
        this.index = index;
        this.vector = vector;
        this.type = type;
    }

}
