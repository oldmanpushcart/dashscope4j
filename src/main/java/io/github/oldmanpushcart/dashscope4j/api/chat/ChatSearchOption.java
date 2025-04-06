package io.github.oldmanpushcart.dashscope4j.api.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 搜索选项
 *
 * @since 3.1.0
 */
@Data
@Accessors(fluent = true, chain = true)
public class ChatSearchOption {

    @JsonProperty("enable_source")
    boolean enableSource;

    @JsonProperty("enable_citation")
    boolean enableCitation;

    @JsonProperty("forced_search")
    boolean forcedSearch;

    @JsonProperty("search_strategy")
    SearchStrategy searchStrategy;

    @JsonProperty("citation_format")
    CitationFormat citationFormat;

    /**
     * 搜索策略
     */
    public enum SearchStrategy {

        /**
         * 标准搜索：在请求时搜索5条互联网信息；
         */
        STANDARD,

        /**
         * 高级搜索：在请求时搜索10条互联网信息；
         */
        PRO

    }

    /**
     * 引用格式
     */
    public enum CitationFormat {

        /**
         * 仅引用编号
         * <p>
         * 例子：[1]
         * </p>
         */
        NUMBER_ONLY,

        /**
         * REF开头的引用编号
         * <p>
         * 例子：REF_1
         * </p>
         */
        REF_NUMBER

    }

}
