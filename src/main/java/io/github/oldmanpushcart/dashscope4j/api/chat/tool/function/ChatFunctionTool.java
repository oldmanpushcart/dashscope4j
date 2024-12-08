package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 对话函数工具
 */
@Value
@Accessors(fluent = true)
public class ChatFunctionTool implements Tool {

    @JsonProperty("function")
    Meta meta;

    ChatFunction<?, ?> function;

    @JsonProperty("type")
    @Override
    public Classify classify() {
        return Classify.FUNCTION;
    }

    /**
     * 对话函数调用存根
     */
    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class Call implements Tool.Call {

        /**
         * 函数名称
         */
        @JsonProperty(

                /*
                 * 这里使用WRITE_ONLY，表示只在序列化时输出，反序列化时忽略、
                 * 原因是在 #extract() 方法中已经包含了参数的提取逻辑，
                 */
                access = JsonProperty.Access.WRITE_ONLY

        )
        String name;

        /**
         * 函数入参（JSON格式）
         */
        @JsonProperty(

                /*
                 * 这里使用WRITE_ONLY，表示只在序列化时输出，反序列化时忽略、
                 * 原因是在 #extract() 方法中已经包含了参数的提取逻辑，
                 */
                access = JsonProperty.Access.WRITE_ONLY

        )
        String arguments;

        /**
         * @return 工具调用存根类型（函数存根）
         */
        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        @JsonProperty("function")
        Map<?, ?> extract() {
            return new HashMap<Object, Object>() {{
                put("name", name);
                put("arguments", arguments);
            }};
        }

    }

    /**
     * 函数元数据
     */
    @Value
    @Accessors(fluent = true)
    public static class Meta implements Tool.Meta {

        /**
         * 函数名称
         */
        @JsonProperty
        String name;

        /**
         * 函数描述
         */
        @JsonProperty
        String description;

        /**
         * 函数入参元数据
         */
        @JsonProperty("parameters")
        TypeSchema parameterTs;

        /**
         * 参数元数据
         */
        @Accessors(fluent = true)
        public static class TypeSchema {

            @Getter
            private final Type type;

            @Getter
            private final String schema;

            @JsonValue
            private final JsonNode node;

            public TypeSchema(Type type) {
                final JsonNode node = JacksonUtils.schema(type);
                final String schema = JacksonUtils.toJson(node);
                this.type = type;
                this.node = node;
                this.schema = schema;
            }

            public TypeSchema(Type type, String schema) {
                final JsonNode node = JacksonUtils.toNode(schema);
                this.type = type;
                this.node = node;
                this.schema = schema;
            }

        }

    }

    /**
     * 通过注解构建函数工具
     *
     * @param function 函数
     * @return 函数工具
     */
    public static ChatFunctionTool of(ChatFunction<?, ?> function) {
        return ChatFunctionToolHelper.parse(function);
    }


}
