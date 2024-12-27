package io.github.oldmanpushcart.dashscope4j.api.embedding.text;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;

public interface EmbeddingOptions {

    /**
     * TEXT_TYPE
     * <p>
     * 文本转换为向量后可以应用于检索、聚类、分类等下游任务，对检索这类非对称任务为了达到更好的检索效果建议区分：
     *     <ul>
     *         <li>查询文本：{@link TextType#QUERY}</li>
     *         <li>底库文本：{@link TextType#DOCUMENT}</li>
     *     </ul>
     * </p>
     * <p>聚类、分类等对称任务可以不用特殊指定，采用系统默认值"document"即可 </p>
     */
    Option.SimpleOpt<TextType> TEXT_TYPE = new Option.SimpleOpt<>("text_type", TextType.class);

    /**
     * 向量计算类型
     */
    enum TextType {

        /**
         * 查询
         * <p>应用于检索</p>
         */
        @JsonProperty("query")
        QUERY,

        /**
         * 文档
         * <p>应用于底库</p>
         */
        @JsonProperty("document")
        DOCUMENT

    }

}
