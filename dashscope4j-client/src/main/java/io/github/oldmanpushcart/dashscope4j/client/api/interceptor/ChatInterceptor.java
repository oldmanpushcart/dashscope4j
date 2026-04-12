package io.github.oldmanpushcart.dashscope4j.client.api.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;

import java.util.concurrent.CompletionStage;

/**
 * 聊天模型拦截器
 * <p>
 * 专门用于拦截和处理聊天模型(ChatModel)的请求。
 * 该拦截器会自动过滤非聊天模型的请求,只对聊天模型请求进行拦截处理。
 * </p>
 */
public interface ChatInterceptor extends Interceptor {

    /**
     * 拦截链处理方法(默认实现)
     * <p>
     * 该方法会检查请求是否为聊天模型请求:
     * - 如果不是聊天模型请求,则直接放行
     * - 如果是聊天模型请求,则调用 {@link #intercept(Chain, AigcRequest)} 进行拦截处理
     * </p>
     *
     * @param chain 拦截器链
     * @return 处理结果的 CompletionStage
     */
    @Override
    default CompletionStage<?> intercept(Chain chain) {
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }
        final var request = aigcRequest.as(model);
        return intercept(chain, request);
    }

    /**
     * 拦截聊天模型请求
     * <p>
     * 子类需要实现此方法来定义具体拦截逻辑,
     * 可以在请求发送前进行修改、验证或增强等操作。
     * </p>
     *
     * @param chain   拦截器链,用于继续执行后续拦截器或最终请求
     * @param request 聊天模型请求,包含输入和输出类型信息
     * @return 处理结果的 CompletionStage
     */
    CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request);

}
