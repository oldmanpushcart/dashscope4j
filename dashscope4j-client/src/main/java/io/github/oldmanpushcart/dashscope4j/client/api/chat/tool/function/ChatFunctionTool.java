package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.lang.reflect.Type;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 对话函数工具
 */
@Value
@Accessors(fluent = true)
public class ChatFunctionTool implements Tool {

    @JsonProperty("function")
    Meta meta;

    ChatFunction<?, ?> function;

    @Override
    public boolean isEnabled() {
        return function.isEnabled();
    }

    @JsonProperty("type")
    @Override
    public Classify classify() {
        return Classify.FUNCTION;
    }

    /**
     * 调用函数
     *
     * @param caller       调用者
     * @param argumentJson 参数JSON
     * @return 调用结果JSON
     */
    public CompletionStage<String> call(ChatFunction.Caller caller, String argumentJson) {
        final Type parameterType = meta.parameterTs().type();
        return function
                .call(caller, toArgument(argumentJson, parameterType))
                .thenApply(JacksonJsonUtils::toJson);
    }

    /*
     * 转换为参数
     * 这里需要处理传递的参数直接为null的情况，null -> null
     * 不要拿null到jackson进行转换
     */
    private static <T> T toArgument(String parameterJson, Type parameterType) {
        if (null == parameterJson || parameterJson.trim().isEmpty()) {
            return null;
        }
        return JacksonJsonUtils.toObject(parameterJson, parameterType);
    }

    /**
     * 对话函数调用存根
     */
    @Value
    @Accessors(fluent = true)
    @AllArgsConstructor
    @lombok.Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class Call implements Tool.Call {

        @JsonProperty("index")
        int index;

        @JsonProperty("id")
        String id;

        @JsonProperty("function")
        Stub stub;

        /**
         * @return 工具调用存根类型（函数存根）
         */
        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        @Value
        @Accessors(fluent = true)
        @AllArgsConstructor
        @lombok.Builder(access = AccessLevel.PRIVATE)
        @Jacksonized
        public static class Stub {

            @JsonProperty
            String name;

            @JsonProperty
            String arguments;

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
                final JsonNode node = JacksonJsonUtils.schema(type);
                final String schema = JacksonJsonUtils.toJson(node);
                this.type = type;
                this.node = node;
                this.schema = schema;
            }

            public TypeSchema(Type type, String schema) {
                final JsonNode node = JacksonJsonUtils.toNode(schema);
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


    /**
     * @return 函数工具构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 函数工具构建器
     */
    public static class Builder implements Buildable<ChatFunctionTool, Builder> {

        private String name;
        private String description;
        private Meta.TypeSchema parameterTs;
        private ChatFunction<?, ?> function;

        public Builder name(String name) {
            this.name = requireNonBlankString(name, "Function name must not be blank");
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parameterType(Type type) {
            requireNonNull(type, "Parameter type must not be null");
            this.parameterTs = new Meta.TypeSchema(type);
            return this;
        }

        public Builder parameterType(Type type, String schema) {
            requireNonNull(type, "Parameter type must not be null");
            requireNonBlankString(schema, "Parameter schema must not be blank");
            this.parameterTs = new Meta.TypeSchema(type, schema);
            return this;
        }

        public Builder function(ChatFunction<?, ?> function) {
            requireNonNull(function, "Function must not be null");
            this.function = function;
            return this;
        }

        @Override
        public ChatFunctionTool build() {
            requireNonNull(name, "Function name must not be null");
            requireNonNull(parameterTs, "Parameter type must not be null");
            requireNonNull(function, "Function must not be null");
            return new ChatFunctionTool(
                    new Meta(
                            name,
                            description,
                            parameterTs
                    ),
                    function
            );
        }

    }

}
