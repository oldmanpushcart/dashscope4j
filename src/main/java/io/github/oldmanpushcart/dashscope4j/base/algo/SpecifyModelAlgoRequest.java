package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;

/**
 * 指定模型算法请求
 * <p>
 * 因为{@link AlgoRequest}没有办法指定{@link Model}，
 * 为了考虑API的向后兼容，所以这里重新创建了一个{@link SpecifyModelAlgoRequest}来解决这个问题。
 * </p>
 *
 * @param <M> 模型类型
 * @param <R> 响应类型
 * @since 1.4.0
 */
public interface SpecifyModelAlgoRequest<M extends Model, R extends AlgoResponse<?>>
        extends AlgoRequest<R> {

    /**
     * 获取模型
     *
     * @return 模型
     */
    M model();

    /**
     * 指定模型算法请求构建器
     *
     * @param <M> 模型类型
     * @param <T> 请求类型
     * @param <B> 构建器类型
     */
    interface Builder<M extends Model, T extends SpecifyModelAlgoRequest<M, ?>, B extends Builder<M, T, B>>
            extends AlgoRequest.Builder<M, T, B> {

    }

}
