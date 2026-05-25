package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.SchemaUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils.requireNonBlankString;
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
        if (null == argumentJson) {
            return null;
        }
        try {
            return JacksonJsonUtils.toObject(argumentJson, parameterType);
        } catch (Throwable cause) {
            throw ToolExecutionException.marshalFailed(meta.name(), cause);
        }
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> {

                    // 1. 执行核心逻辑，获取原始结果 (可能是 T 或 CompletionStage<T>)
                    final var rawResult = function.apply(caller, toArgument(argumentJson));

                    // 2. 统一转换为 CompletionStage 进行处理
                    //noinspection unchecked
                    return (rawResult instanceof CompletionStage<?>)
                            ? (CompletionStage<Object>) rawResult
                            : CompletableFuture.completedFuture(rawResult);
                })
                .thenApply(JacksonJsonUtils::toJson)
                .handle((r, ex) -> {

                    if (null == ex) {
                        return CompletableFuture.completedStage(r);
                    }

                    final var cause = CompletableFutureUtils.unwrapEx(ex);
                    final var toolEx = (cause instanceof ToolExecutionException teCause)
                            ? teCause
                            : ToolExecutionException.callFailed(meta.name(), cause);
                    return CompletableFuture.<String>failedStage(toolEx);

                })
                .thenCompose(v -> v);
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
