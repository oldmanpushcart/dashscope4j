package io.github.oldmanpushcart.dashscope4j.client.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import java.util.concurrent.CompletionStage;

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
     * 调用工具
     *
     * @param caller       调用者
     * @param argumentJson 参数（JSON格式）
     * @return 调用结果（JSON格式）
     */
    CompletionStage<String> call(Caller caller, String argumentJson);

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

}
