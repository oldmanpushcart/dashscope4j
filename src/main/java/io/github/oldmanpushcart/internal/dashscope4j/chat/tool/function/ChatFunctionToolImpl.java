package io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.internal.dashscope4j.util.GenericReflectUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

public record ChatFunctionToolImpl(Meta meta, ChatFunction<?, ?> function) implements ChatFunctionTool {

    @JsonProperty("type")
    @Override
    public Classify classify() {
        return Classify.FUNCTION;
    }

    @JsonProperty("function")
    @Override
    public Meta meta() {
        return meta;
    }

    public record MetaImpl(

            @JsonProperty("name")
            String name,

            @JsonProperty("description")
            String description,

            @JsonProperty("parameters")
            TypeSchema parameterTs

    ) implements Meta {
    }

    public record CallImpl(

            @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
            String name,

            @JsonProperty(value = "arguments", access = JsonProperty.Access.WRITE_ONLY)
            String arguments

    ) implements Call {

        @JsonProperty("type")
        @Override
        public Classify classify() {
            return Classify.FUNCTION;
        }

        @JsonProperty("function")
        Map<?, ?> extract() {
            return Map.of(
                    "name", name,
                    "arguments", arguments
            );
        }

    }

    public record TypeSchemaImpl(Type type, String schema, JsonNode node) implements Meta.TypeSchema {

        @JsonValue
        JsonNode extract() {
            return node;
        }

        static Meta.TypeSchema ofType(Type type) {
            final var node = JacksonUtils.schema(type);
            final var schema = JacksonUtils.toJson(node);
            return new TypeSchemaImpl(type, schema, node);
        }

        static Meta.TypeSchema ofType(Type type, String schema) {
            final var node = JacksonUtils.toNode(schema);
            return new TypeSchemaImpl(type, schema, node);
        }

    }


    /**
     * 通过注解构建函数工具
     *
     * @param function 函数
     * @return 函数工具
     */
    public static ChatFunctionTool byAnnotation(ChatFunction<?, ?> function) {

        // 获取函数类
        final var functionClass = function.getClass();

        // 检查是否实现ChatFunction接口
        if (!ChatFunction.class.isAssignableFrom(functionClass)) {
            throw new IllegalArgumentException("required implements interface: %s".formatted(
                    ChatFunction.class.getName()
            ));
        }

        // 检查是否有注解
        if (!functionClass.isAnnotationPresent(ChatFn.class)) {
            throw new IllegalArgumentException("required annotation: %s".formatted(
                    ChatFn.class.getName()
            ));
        }

        // 找到ChatFunction接口
        final var interfaceType = Optional.ofNullable(GenericReflectUtils.findFirst(functionClass, ChatFunction.class))
                .orElseThrow(() -> new IllegalArgumentException("required implements interface: %s".formatted(
                        ChatFunction.class.getName()
                )));

        final var anChatFn = functionClass.getAnnotation(ChatFn.class);
        final var parameterType = interfaceType.getActualTypeArguments()[0];

        return new ChatFunctionToolImpl(
                new MetaImpl(
                        anChatFn.name(),
                        anChatFn.description(),
                        TypeSchemaImpl.ofType(parameterType)
                ),
                function
        );
    }

}
