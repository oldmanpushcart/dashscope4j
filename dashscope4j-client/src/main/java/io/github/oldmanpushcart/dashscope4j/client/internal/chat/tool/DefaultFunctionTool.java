package io.github.oldmanpushcart.dashscope4j.client.internal.chat.tool;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.oldmanpushcart.dashscope4j.client.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.SchemaUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils.toObject;
import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class DefaultFunctionTool implements FunctionTool {

    private final Meta meta;
    private final BiFunction<Tool.Caller, ?, ?> function;
    private final Type parameterType;

    private DefaultFunctionTool(Builder builder) {
        requireNonBlankString(builder.name, "name must not be blank!");
        requireNonNull(builder.parameterType, "parameterType must not be null!");
        requireNonNull(builder.function, "function must not be null!");
        this.meta = newMeta(builder);
        this.function = builder.function;
        this.parameterType = builder.parameterType;
    }

    private static FunctionTool.Meta newMeta(Builder builder) {
        final var parameterSchema = Objects.isNull(builder.parameterSchema)
                ? SchemaUtils.schema(builder.parameterType)
                : builder.parameterSchema;
        return new FunctionTool.Meta(
                builder.name,
                builder.description,
                parameterSchema
        );
    }

    @Override
    public Meta meta() {
        return meta;
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
        try {
            final var result = function.apply(caller, toObject(argumentJson, parameterType));
            if(result instanceof CompletionStage<?> stage) {
                return stage.thenApply(JacksonJsonUtils::toJson);
            } else {
                final var resultJson = JacksonJsonUtils.toJson(result);
                return CompletableFuture.completedFuture(resultJson);
            }
        } catch (Throwable ex) {
            return CompletableFuture.failedStage(ex);
        }
    }

    public static class Builder implements FunctionTool.Builder {

        private String name;
        private String description;
        private BiFunction<Tool.Caller, ?, ?> function;
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
