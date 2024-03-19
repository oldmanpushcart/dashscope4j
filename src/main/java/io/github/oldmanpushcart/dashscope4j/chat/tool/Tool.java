package io.github.oldmanpushcart.dashscope4j.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 工具
 *
 * @since 1.2.0
 */
public interface Tool {

    /**
     * @return 工具分类
     */
    Classify classify();

    /**
     * 分类
     *
     * @since 1.2.0
     */
    enum Classify {

        @JsonProperty("function")
        FUNCTION

    }

    /**
     * 工具调用
     *
     * @since 1.2.0
     */
    interface Call {

        /**
         * @return 工具分类
         */
        Classify classify();

    }

}
