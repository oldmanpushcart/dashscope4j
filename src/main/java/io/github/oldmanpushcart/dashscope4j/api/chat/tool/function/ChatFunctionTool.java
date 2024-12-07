package io.github.oldmanpushcart.dashscope4j.api.chat.tool.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.util.GenericReflectUtils;
import io.github.oldmanpushcart.dashscope4j.util.JacksonUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class Call implements Tool.Call {

        @JsonProperty
        String name;

        @JsonProperty
        String arguments;

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

    @Value
    @Accessors(fluent = true)
    public static class Meta implements Tool.Meta {

        @JsonProperty
        String name;

        @JsonProperty
        String description;

        @JsonProperty
        TypeSchema parameterTs;

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

        // 获取函数类
        final Class<?> functionClass = getFunctionClass(function);

        // 找到ChatFunction接口
        final ParameterizedType interfaceType = Optional
                .ofNullable(GenericReflectUtils.findFirst(functionClass, ChatFunction.class))
                .orElseThrow(() -> new IllegalArgumentException(String.format("required implements interface: %s",
                        ChatFunction.class.getName()
                )));

        final ChatFn anChatFn = functionClass.getAnnotation(ChatFn.class);
        final Type parameterType = interfaceType.getActualTypeArguments()[0];

        return new ChatFunctionTool(
                new Meta(
                        anChatFn.name(),
                        anChatFn.description(),
                        new Meta.TypeSchema(parameterType)
                ),
                function
        );
    }

    private static Class<?> getFunctionClass(ChatFunction<?, ?> function) {
        final Class<?> functionClass = function.getClass();

        // 检查是否实现ChatFunction接口
        if (!ChatFunction.class.isAssignableFrom(functionClass)) {
            throw new IllegalArgumentException(String.format("required implements interface: %s",
                    ChatFunction.class.getName()
            ));
        }

        // 检查是否有注解
        if (!functionClass.isAnnotationPresent(ChatFn.class)) {
            throw new IllegalArgumentException(String.format("required annotation: %s",
                    ChatFunction.class.getName()
            ));
        }
        return functionClass;
    }

}
