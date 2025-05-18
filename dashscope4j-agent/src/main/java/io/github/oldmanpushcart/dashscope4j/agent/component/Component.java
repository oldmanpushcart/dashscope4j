package io.github.oldmanpushcart.dashscope4j.agent.component;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * 智能体组件
 * <p>
 * {@link ChatAgent} 的组件，用于对对话进行增强。
 * </p>
 */
public interface Component {

    /**
     * 异步对话
     *
     * @param processor 处理器
     * @return 处理结果
     */
    CompletionStage<ChatResponse> onAsync(Processor<ChatResponse> processor);

    /**
     * 流式对话
     *
     * @param processor 处理器
     * @return 处理结果
     */
    CompletionStage<Flowable<ChatResponse>> onFlow(Processor<Flowable<ChatResponse>> processor);

    /**
     * 处理器
     *
     * @param <R> 处理结果类型
     */
    interface Processor<R> {

        /**
         * @return 对话请求
         */
        ChatRequest request();

        /**
         * 处理对话请求
         *
         * @param request 对话请求
         * @return 处理结果
         */
        CompletionStage<R> process(ChatRequest request);

    }

}
