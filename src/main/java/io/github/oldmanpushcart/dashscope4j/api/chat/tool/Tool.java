package io.github.oldmanpushcart.dashscope4j.api.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;

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
     * 工具调用存根
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ChatFunctionTool.Call.class, name = "function")
    })
    interface Call {

        /**
         * @return 调用ID
         * @since 3.1.0
         */
        String id();

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
