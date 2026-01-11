package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.tool.DefaultFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.joinStrings;

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

    ) {

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

            // 合并
            return new Call(
                    index,
                    joinStrings(id, next.id()),
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
                        joinStrings(name, next.name),
                        joinStrings(arguments, next.arguments)
                );
            }

        }

    }


    static Builder newBuilder() {
        return new DefaultFunctionTool.Builder();
    }

    interface Builder extends Buildable<FunctionTool, Builder> {

        Builder name(String name);

        Builder description(String description);

        <T> Builder function(BiFunction<Tool.Caller, T, ?> function);

        <T> Builder function(Function<T, ?> function);

        Builder parameterType(Type parameterType);

        Builder parameterSchema(JsonNode parameterSchema);

    }

}
