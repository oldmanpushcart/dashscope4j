package io.github.oldmanpushcart.dashscope4j.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 工具
 */
public interface Tool {

    /**
     * @return 工具分类
     */
    Classify classify();

    /**
     * 分类
     */
    enum Classify {

        @JsonProperty("function")
        FUNCTION

    }

    /**
     * 工具调用
     */
    interface Call {

        /**
         * @return 工具分类
         */
        Classify classify();

    }

    /**
     * 工具元数据
     */
    interface Meta {

    }

}
