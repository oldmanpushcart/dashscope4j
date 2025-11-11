package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils.concat;

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

            @JsonProperty("name")
            String name,

            @JsonProperty("description")
            String description,

            @JsonProperty("parameters")
            JsonNode parameterSchema

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

            @JsonProperty("index")
            int index,

            @JsonProperty("id")
            String id,

            @JsonProperty("function")
            Stub stub

    ) implements Tool.Call {

        /**
         * @return 工具调用存根类型（函数存根）
         */
        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        @Override
        public Tool.Call accumulate(Tool.Call call) {

            // index 必须相等
            if (index != call.index()) {
                throw new IllegalArgumentException("except index : %s but was: %s".formatted(index, index));
            }

            // 类型必须一致
            if (!(call instanceof Call next)) {
                throw new IllegalArgumentException("Not a function tool call");
            }

            // 合并Call
            return new Call(
                    index,
                    concat(id, next.id()),
                    stub().accumulate(next.stub())
            );

        }

        /**
         * 存根信息
         *
         * @param name      函数名称
         * @param arguments 函数参数
         */
        public record Stub(

                @JsonProperty("name")
                String name,

                @JsonProperty("arguments")
                String arguments

        ) implements Accumulator<Stub> {

            @Override
            public Stub accumulate(Stub next) {
                return new Stub(
                        concat(name, next.name),
                        concat(arguments, next.arguments)
                );
            }

        }

    }

}
