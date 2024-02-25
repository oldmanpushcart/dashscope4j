package io.github.ompc.dashscope4j.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.base.algo.AlgoRequest;
import io.github.ompc.internal.dashscope4j.embedding.EmbeddingRequestBuilderImpl;

/**
 * 向量计算请求
 */
public interface EmbeddingRequest extends AlgoRequest<EmbeddingResponse> {

    /**
     * 构建向量计算请求
     *
     * @return 构建器
     */
    static Builder newBuilder() {
        return new EmbeddingRequestBuilderImpl();
    }

    /**
     * 向量计算请求构建器
     */
    interface Builder extends AlgoRequest.Builder<EmbeddingModel, EmbeddingRequest, Builder> {

        /**
         * 添加文档
         *
         * @param documents 文档
         * @return 构建器
         */
        Builder documents(String... documents);

    }

    /**
     * 向量计算类型
     */
    enum EmbeddingType {

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
