package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;

/**
 * 算法请求
 */
public interface AlgoRequest<R extends AlgoResponse<?>> extends ApiRequest<R> {

    /**
     * @return 协议
     * @since 1.4.3
     */
    @Override
    default String protocol() {
        return "%s/%s".formatted(ApiRequest.super.protocol(), model().name());
    }

    /**
     * 获取模型
     *
     * @return 模型
     */
    Model model();

    /**
     * 获取输入
     *
     * @return 输入
     */
    Object input();

    /**
     * 获取选项
     *
     * @return 选项
     */
    Option option();

    /**
     * 算法请求构建器
     *
     * @param <M> 模型
     * @param <T> 请求
     * @param <B> 构建器
     */
    interface Builder<M extends Model, T extends AlgoRequest<?>, B extends Builder<M, T, B>> extends ApiRequest.Builder<T, B> {

        /**
         * 设置模型
         *
         * @param model 模型
         * @return this
         */
        B model(M model);

        /**
         * 设置选项
         *
         * @param opt   选项
         * @param value 选项值
         * @param <OT>  选项类型
         * @param <OR>  选项值类型
         * @return this
         */
        <OT, OR> B option(Option.Opt<OT, OR> opt, OT value);

        /**
         * 设置选项
         *
         * @param name  选项名
         * @param value 选项值
         * @return this
         */
        B option(String name, Object value);

    }

}
