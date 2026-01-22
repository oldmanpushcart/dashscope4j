package io.github.oldmanpushcart.dashscope4j.client.aigc;

import io.github.oldmanpushcart.dashscope4j.client.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.GenericReflectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Set;

public interface AigcModel<I, O> extends Model {

    default Set<String> tags() {
        return Set.of();
    }

    default List<Interceptor> interceptors() {
        return List.of();
    }

    default Class<I> inputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<I>) parameterizedType.getActualTypeArguments()[0];
    }

    default Class<O> outputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<O>) parameterizedType.getActualTypeArguments()[1];
    }

}
