package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.component.Component;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 集成了组件的对话操作
 */
@AllArgsConstructor
class BaseChatOp implements ChatOp {

    private final ChatAgent agent;
    private final ChatOp chatOp;
    private final Component component;

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return component.onAsync(new ProcessorImpl<>(agent, request, chatOp::async));
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return component.onFlow(new ProcessorImpl<>(agent, request, chatOp::flow));
    }

    /**
     * 创建对话操作
     *
     * @param agent      初始对话操作
     * @param components 组件集合
     * @return 对话操作
     */
    public static ChatOp of(BaseChatAgent agent, List<Component> components) {
        return of(agent, new ChatOpImpl(agent), components);
    }

    /**
     * 创建对话操作
     *
     * @param agent      智能体
     * @param chatOp     初始对话操作
     * @param components 组件集合
     * @return 对话操作
     */
    private static ChatOp of(ChatAgent agent, ChatOp chatOp, List<Component> components) {
        final List<Component> clones = new ArrayList<>(components);
        Collections.reverse(clones);
        ChatOp op = chatOp;
        for (final Component chain : clones) {
            op = new BaseChatOp(agent, op, chain);
        }
        return op;
    }

    /**
     * ChatOp 内部实现
     */
    @AllArgsConstructor
    private static class ChatOpImpl implements ChatOp {

        private final BaseChatAgent agent;

        @Override
        public CompletionStage<ChatResponse> async(ChatRequest request) {
            return agent.baseAsync(request);
        }

        @Override
        public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
            return agent.baseFlow(request);
        }

    }

    /**
     * 处理器内部实现
     *
     * @param <R> 返回类型
     */
    @Accessors(fluent = true)
    @AllArgsConstructor
    private static final class ProcessorImpl<R> implements Component.Processor<R> {

        @Getter
        private final ChatAgent agent;

        @Getter
        private final ChatRequest request;

        private final Function<ChatRequest, CompletionStage<R>> operator;

        @Override
        public CompletionStage<R> process(ChatRequest request) {
            return operator.apply(request);
        }

    }

}
