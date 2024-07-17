package io.github.oldmanpushcart.internal.dashscope4j.chat.tool.function;

import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunctionTool;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class ChatFunctionToolBuilderImpl implements ChatFunctionTool.Builder {

    private String name;
    private String description;
    private ChatFunctionTool.Meta.TypeSchema parameterTs;
    private ChatFunction<?, ?> function;

    @Override
    public ChatFunctionTool.Builder name(String name) {
        this.name = requireNonBlankString(name, "name is blank");
        return this;
    }

    @Override
    public ChatFunctionTool.Builder description(String description) {
        this.description = requireNonNull(description);
        return this;
    }

    @Override
    public ChatFunctionTool.Builder parameterType(Type parameterType) {
        requireNonNull(parameterType);
        this.parameterTs = ChatFunctionToolImpl.TypeSchemaImpl.ofType(parameterType);
        return this;
    }

    @Override
    public ChatFunctionTool.Builder parameterType(Type parameterType, String schema) {
        requireNonNull(parameterType);
        requireNonBlankString(schema, "schema is blank");
        this.parameterTs = ChatFunctionToolImpl.TypeSchemaImpl.ofType(parameterType, schema);
        return this;
    }

    @Override
    public <T, R> ChatFunctionTool.Builder chatFunction(ChatFunction<T, R> function) {
        this.function = requireNonNull(function);
        return this;
    }

    @Override
    public <T, R> ChatFunctionTool.Builder function(Function<T, R> function) {
        requireNonNull(function);
        this.function = (ChatFunction<T, R>) t -> CompletableFuture.completedFuture(function.apply(t));
        return this;
    }

    @Override
    public ChatFunctionTool build() {
        requireNonBlankString(name, "name is blank");
        requireNonNull(parameterTs);
        return new ChatFunctionToolImpl(
                new ChatFunctionToolImpl.MetaImpl(name, description, parameterTs),
                requireNonNull(function)
        );
    }

}
