package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.util.concurrent.CompletionStage;

/**
 * 工具
 */
public interface Tool {

    /**
     * @return 工具是否可用
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * @return 工具分类
     */
    Classify classify();

    /**
     * 调用工具
     *
     * @param caller   调用者
     * @param argument 参数
     * @return 调用结果
     */
    CompletionStage<String> call(Caller caller, String argument);

    /**
     * 调用者
     */
    interface Caller {

        /**
         * @return 触发对话请求
         */
        ChatRequest request();

    }

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
            @JsonSubTypes.Type(value = FunctionTool.Call.class, name = "function")
    })
    interface Call extends Accumulator<Call> {

        /**
         * @return INDEX
         */
        int index();

        /**
         * @return ID
         */
        String id();

        /**
         * @return CLASSIFY
         */
        Classify classify();

    }

    /**
     * 工具元数据
     */
    interface Meta {

    }

}
