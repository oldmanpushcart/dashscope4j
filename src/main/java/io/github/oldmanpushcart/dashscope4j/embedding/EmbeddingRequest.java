package io.github.oldmanpushcart.dashscope4j.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.embedding.EmbeddingRequestBuilderImpl;

import java.util.List;

/**
 * 向量计算请求
 */
public interface EmbeddingRequest extends SpecifyModelAlgoRequest<EmbeddingModel, EmbeddingResponse> {

    /**
     * @return 文档列表
     * @since 1.4.0
     */
    List<String> documents();

    /**
     * 构建向量计算请求
     *
     * @return 构建器
     */
    static Builder newBuilder() {
        return new EmbeddingRequestBuilderImpl();
    }

    /**
     * 构建向量计算请求
     *
     * @param request 请求
     * @return 构建器
     * @since 1.4.0
     */
    static Builder newBuilder(EmbeddingRequest request) {
        return new EmbeddingRequestBuilderImpl(request);
    }

    /**
     * 向量计算请求构建器
     */
    interface Builder extends SpecifyModelAlgoRequest.Builder<EmbeddingModel, EmbeddingRequest, Builder> {

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
