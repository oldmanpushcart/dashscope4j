package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.api.intercetpor.Interceptor;

import java.util.List;
import java.util.Set;

/**
 * 算法模型
 *
 * @param <I> 输入类型
 * @param <O> 输出类型
 */
public interface AigcModel<I, O> extends Model<I, O> {

    /**
     * 模型标签
     * <p>
     * 标签标记了模型的特征，有些模型只能用{@code task}，有的只能用增量流式输出，都可以通过标签来表达。
     * 模型的拦截器会根据标签来对模型进行功能增强。
     * </p>
     *
     * @return 模型标签集合
     */
    default Set<String> tags() {
        return Set.of();
    }

    /**
     * 模型拦截链
     * <p>
     * 模型会有一些调用前后的特殊处理需求，可以通过拦截器实现。
     * 这些与模型绑定的拦截器可以在这里声明注册。
     * </p>
     *
     * @return 拦截链
     */
    default List<Interceptor> interceptors() {
        return List.of();
    }

}
