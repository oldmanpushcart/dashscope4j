package io.github.oldmanpushcart.dashscope4j.client.aigc;

import com.fasterxml.jackson.annotation.JsonValue;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.GenericReflectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.Set;

public interface Model<I, O> {

    @JsonValue
    String name();

    String path();

    default Set<String> tags() {
        return Set.of();
    }

    default Set<Interceptor> interceptors() {
        return Set.of();
    }

    default Class<I> inputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), Model.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<I>) parameterizedType.getActualTypeArguments()[0];

    }

    default Class<O> outputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), Model.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<O>) parameterizedType.getActualTypeArguments()[1];
    }

}
