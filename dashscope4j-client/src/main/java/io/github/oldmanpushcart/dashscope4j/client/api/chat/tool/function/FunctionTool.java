package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;

/**
 * 函数工具
 */
public interface FunctionTool extends Tool {

    @JsonProperty("function")
    Meta meta();

    /**
     * @return 工具类型
     */
    @JsonProperty("type")
    @Override
    default Classify classify() {
        return Classify.FUNCTION;
    }

    /**
     * 函数元数据
     *
     * @param name            函数名称
     * @param description     函数描述
     * @param parameterSchema 函数参数约束描述
     */
    record Meta(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("parameters") JsonNode parameterSchema
    ) implements Tool.Meta {

    }

    /**
     * 函数调用存根
     *
     * @param index 调用序号
     * @param id    调用编号
     * @param stub  函数存根
     */
    record Call(
            @JsonProperty("index") int index,
            @JsonProperty("id") String id,
            @JsonProperty("function") Stub stub
    ) implements Tool.Call {

        /**
         * @return 工具调用存根类型（函数存根）
         */
        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        /**
         * 存根信息
         *
         * @param name      函数名称
         * @param arguments 函数参数
         */
        public record Stub(
                @JsonProperty("name") String name,
                @JsonProperty("arguments") String arguments
        ) {

        }

    }

}
