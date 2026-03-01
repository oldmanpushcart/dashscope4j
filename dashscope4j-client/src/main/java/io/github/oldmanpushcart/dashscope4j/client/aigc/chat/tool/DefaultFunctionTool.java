package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.SchemaUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

class DefaultFunctionTool implements FunctionTool {

    private final Meta meta;
    private final BiFunction<Caller, ?, ?> function;
    private final Type parameterType;

    private DefaultFunctionTool(Builder builder) {
        requireNonBlankString(builder.name, "name must not be blank!");
        requireNonNull(builder.parameterType, "parameterType must not be null!");
        requireNonNull(builder.function, "function must not be null!");
        this.meta = newMeta(builder);
        this.function = builder.function;
        this.parameterType = builder.parameterType;
    }

    private static Meta newMeta(Builder builder) {
        final var parameterSchema = Objects.isNull(builder.parameterSchema)
                ? SchemaUtils.schema(builder.parameterType)
                : builder.parameterSchema;
        return new Meta(
                builder.name,
                builder.description,
                parameterSchema
        );
    }

    @Override
    public Meta meta() {
        return meta;
    }

    private <T> T toArgument(String argumentJson) {
        try {
            return JacksonJsonUtils.toObject(argumentJson, parameterType);
        } catch (Throwable cause) {
            throw ToolException.unmarshalFailed(meta.name(), cause);
        }
    }

    private String toResultJson(Object result) {
        try {
            return JacksonJsonUtils.toJson(result);
        } catch (Throwable cause) {
            throw ToolException.marshalFailed(meta.name(), cause);
        }
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
        try {
            final var result = function.apply(caller, toArgument(argumentJson));

            // 处理异步返回
            if (result instanceof CompletionStage<?> stage) {
                return stage
                        .thenApply(this::toResultJson)
                        .handle((r, ex) -> {
                            if (null == ex) {
                                return CompletableFuture.completedStage(r);
                            } else {
                                final var toolEx = ToolException.callFailed(meta.name(), ex);
                                return CompletableFuture.<String>failedStage(toolEx);
                            }
                        })
                        .thenCompose(v -> v);
            }

            // 处理同步返回
            else {
                final var resultJson = toResultJson(result);
                return CompletableFuture.completedFuture(resultJson);
            }
        } catch (ToolException toolEx) {
            return CompletableFuture.failedStage(toolEx);
        } catch (Throwable ex) {
            final var toolEx = ToolException.callFailed(meta.name(), ex);
            return CompletableFuture.failedStage(toolEx);
        }
    }

    public static class Builder implements FunctionTool.Builder {

        private String name;
        private String description;
        private BiFunction<Caller, ?, ?> function;
        private Type parameterType;
        private JsonNode parameterSchema;

        @Override
        public FunctionTool.Builder name(String name) {
            requireNonBlankString(name, "Name must not be blank");
            this.name = name;
            return this;
        }

        @Override
        public FunctionTool.Builder description(String description) {
            requireNonBlankString(description, "description must not be blank!");
            this.description = description;
            return this;
        }

        @Override
        public FunctionTool.Builder supplier(Supplier<?> supplier) {
            requireNonNull(supplier, "supplier must not be null!");
            this.function = (BiFunction<Caller, Object, Object>) (caller, arg) -> supplier.get();
            return parameterType(Object.class);
        }

        @Override
        public FunctionTool.Builder supplier(Function<Caller, ?> supplier) {
            requireNonNull(supplier, "supplier must not be null!");
            this.function = (BiFunction<Caller, Object, Object>) (caller, arg) -> supplier.apply(caller);
            return parameterType(Object.class);
        }

        @Override
        public <T> FunctionTool.Builder function(BiFunction<Caller, T, ?> function) {
            requireNonNull(function, "function must not be null!");
            this.function = function;
            return this;
        }

        @Override
        public <T> FunctionTool.Builder function(Function<T, ?> function) {
            requireNonNull(function, "function must not be null!");
            this.function = (BiFunction<Caller, T, Object>) (caller, t) -> function.apply(t);
            return this;
        }


        @Override
        public FunctionTool.Builder parameterType(Type parameterType) {
            requireNonNull(parameterType, "parameterType must not be null!");
            this.parameterType = parameterType;
            return this;
        }

        @Override
        public FunctionTool.Builder parameterSchema(JsonNode parameterSchema) {
            requireNonNull(parameterSchema, "parameterSchema must not be null!");
            this.parameterSchema = parameterSchema;
            return this;
        }

        @Override
        public FunctionTool build() {
            return new DefaultFunctionTool(this);
        }

    }

}
