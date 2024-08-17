package io.github.oldmanpushcart.dashscope4j.embedding.text;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.HttpAlgoRequest;
import io.github.oldmanpushcart.internal.dashscope4j.embedding.text.EmbeddingRequestBuilderImpl;

import java.util.List;

/**
 * 向量计算请求
 */
public interface EmbeddingRequest extends HttpAlgoRequest<EmbeddingModel, EmbeddingResponse> {

    /**
     * @return 文档列表
     */
    List<String> documents();

    /**
     * @return 构建向量计算请求
     */
    static Builder newBuilder() {
        return new EmbeddingRequestBuilderImpl();
    }

    /**
     * 构建向量计算请求
     *
     * @param request 请求
     * @return 构建器
     */
    static Builder newBuilder(EmbeddingRequest request) {
        return new EmbeddingRequestBuilderImpl(request);
    }

    /**
     * 向量计算请求构建器
     */
    interface Builder extends AlgoRequest.Builder<EmbeddingModel, EmbeddingRequest, Builder> {

        /**
         * 设置文档集合
         *
         * @param documents 文档集合
         * @return 构建器
         */
        Builder documents(List<String> documents);

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
