package io.github.oldmanpushcart.internal.dashscope4j.chat.tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.chat.function.ChatFn;
import io.github.oldmanpushcart.dashscope4j.chat.function.ChatFunction;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

public record FunctionTool(Meta meta, ChatFunction<?, ?> function) implements Tool {

    @Override
    public Classify classify() {
        return Classify.FUNCTION;
    }

    @JsonProperty("function")
    public Meta meta() {
        return meta;
    }

    public record Meta(
            @JsonProperty("name")
            String name,
            @JsonProperty("description")
            String description,
            @JsonProperty("parameters")
            TypeSchema parameterTs,
            @JsonIgnore
            TypeSchema returnTs
    ) {

    }

    record TypeSchema(Type type) {

        @JsonValue
        JsonNode extract() {
            return JacksonUtils.schema(type());
        }

    }

    public record Call(String name, String arguments) implements Tool.Call {

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

    public static FunctionTool of(ChatFunction<?, ?> function) {

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
        final var interfaceType = Stream.of(functionClass.getGenericInterfaces())
                .filter(genericInterface -> genericInterface instanceof ParameterizedType)
                .map(genericInterface -> (ParameterizedType) genericInterface)
                .filter(pType -> pType.getRawType().equals(ChatFunction.class))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("required implements interface: %s".formatted(
                        ChatFunction.class.getName()
                )));

        final var anChatFn = functionClass.getAnnotation(ChatFn.class);
        final var parameterType = interfaceType.getActualTypeArguments()[0];
        final var returnType = interfaceType.getActualTypeArguments()[1];

        return new FunctionTool(
                new Meta(
                        anChatFn.name(),
                        anChatFn.description(),
                        new TypeSchema(parameterType),
                        new TypeSchema(returnType)
                ),
                function
        );
    }

}
