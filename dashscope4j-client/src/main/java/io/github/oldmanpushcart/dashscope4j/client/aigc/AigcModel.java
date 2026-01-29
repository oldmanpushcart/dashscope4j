package io.github.oldmanpushcart.dashscope4j.client.aigc;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.GenericReflectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Set;

public interface AigcModel<I, O> extends Model {

    /**
     * 模型标签
     * <p>
     * 标签标记了模型的特征，有些模型只能用{@code task}，有的只能用增量流式输出，都可以通过标签来表达。
     * 模型的拦截器会根据标签来对模型进行功能增强。
     * </p>
     */
    default Set<String> tags() {
        return Set.of();
    }

    /**
     * @return 模型拦截器列表（所有模型通用）
     */
    default List<Interceptor> interceptors() {
        return List.of();
    }

    /**
     * @return 模型输入参数类型
     */
    default Class<I> inputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<I>) parameterizedType.getActualTypeArguments()[0];
    }

    /**
     * @return 模型输出参数类型
     */
    default Class<O> outputType() {
        final ParameterizedType parameterizedType = GenericReflectUtils.findFirst(getClass(), AigcModel.class);
        //noinspection unchecked
        return parameterizedType == null ? null : (Class<O>) parameterizedType.getActualTypeArguments()[1];
    }

}
