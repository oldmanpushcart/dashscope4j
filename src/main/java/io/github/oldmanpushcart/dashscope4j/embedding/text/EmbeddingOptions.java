package io.github.oldmanpushcart.dashscope4j.embedding.text;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoOptions;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest.EmbeddingType;

public interface EmbeddingOptions extends AlgoOptions {

    /**
     * EMBEDDING_TYPE
     * <p>
     *     文本转换为向量后可以应用于检索、聚类、分类等下游任务，对检索这类非对称任务为了达到更好的检索效果建议区分：
     *     <ul>
     *         <li>查询文本：{@link EmbeddingType#QUERY}</li>
     *         <li>底库文本：{@link EmbeddingType#DOCUMENT}</li>
     *     </ul>
     * </p>
     * <p>聚类、分类等对称任务可以不用特殊指定，采用系统默认值"document"即可 </p>
     */
    Option.SimpleOpt<EmbeddingType> EMBEDDING_TYPE = new Option.SimpleOpt<>("text_type", EmbeddingType.class);

}
