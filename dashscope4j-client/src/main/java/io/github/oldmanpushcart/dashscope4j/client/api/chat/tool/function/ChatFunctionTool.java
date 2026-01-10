package io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.SchemaUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 对话函数工具
 */
public class ChatFunctionTool implements FunctionTool {

    private final Meta meta;

    private final ChatFunction<?, ?> function;
    private final Type parameterType;

    private ChatFunctionTool(Builder builder) {
        requireNonNull(builder.name, "Function name must not be null");
        requireNonNull(builder.function, "Function must not be null");
        requireNonNull(builder.parameterType, "Parameter type must not be null");
        this.meta = newFunctionToolMeta(builder);
        this.function = builder.function;
        this.parameterType = builder.parameterType;
    }

    public Meta meta() {
        return meta;
    }

    private static Meta newFunctionToolMeta(Builder builder) {
        final var parameterSchemaNode = Optional.ofNullable(builder.parameterSchema)
                .map(JacksonJsonUtils::toNode)
                .orElseGet(() -> SchemaUtils.schema(builder.parameterType));
        return new Meta(
                builder.name,
                builder.description,
                parameterSchemaNode
        );
    }

    @Override
    public boolean isEnabled() {
        return function.isEnabled();
    }

    /**
     * 调用函数
     *
     * @param caller       调用者
     * @param argumentJson 参数JSON
     * @return 调用结果JSON
     */
    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
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
        return CommonUtils.isNotBlankString(parameterJson)
                ? JacksonJsonUtils.toObject(parameterJson, parameterType)
                : null;
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
        private ChatFunction<?, ?> function;
        private Type parameterType;
        private String parameterSchema;

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
            this.parameterType = type;
            return this;
        }

        public Builder parameterType(Type type, String schema) {
            requireNonNull(type, "Parameter type must not be null");
            requireNonBlankString(schema, "Parameter schema must not be blank");
            this.parameterType = type;
            this.parameterSchema = schema;
            return this;
        }

        public Builder function(ChatFunction<?, ?> function) {
            requireNonNull(function, "Function must not be null");
            this.function = function;
            return this;
        }

        @Override
        public ChatFunctionTool build() {
            return new ChatFunctionTool(this);
        }

    }

}
