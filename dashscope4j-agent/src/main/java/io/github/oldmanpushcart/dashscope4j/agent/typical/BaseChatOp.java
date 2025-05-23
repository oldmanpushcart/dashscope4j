package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.component.Component;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Value
class BaseChatOp implements ChatOp {

    ChatAgent agent;
    ChatOp chatOp;
    Component component;

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        final Component.Processor<ChatResponse> processor = new ProcessorImpl<>(agent, request, chatOp::async);
        return component.onAsync(processor);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        final Component.Processor<Flowable<ChatResponse>> processor = new ProcessorImpl<>(agent, request, chatOp::flow);
        return component.onFlow(processor);
    }

    /**
     * 创建对话操作
     *
     * @param agent      智能体
     * @param chatOp     初始对话操作
     * @param components 组件集合
     * @return 对话操作
     */
    public static ChatOp of(ChatAgent agent, ChatOp chatOp, List<Component> components) {
        final List<Component> clones = new ArrayList<>(components);
        Collections.reverse(clones);
        ChatOp op = chatOp;
        for (final Component chain : clones) {
            op = new BaseChatOp(agent, op, chain);
        }
        return op;
    }

    /**
     * 创建对话操作
     *
     * @param agent      初始对话操作
     * @param components 组件集合
     * @return 对话操作
     */
    public static ChatOp of(BaseChatAgent agent, List<Component> components) {
        final ChatOp baseChatOp = new ChatOp() {
            @Override
            public CompletionStage<ChatResponse> async(ChatRequest request) {
                return agent.baseAsync(request);
            }

            @Override
            public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
                return agent.baseFlow(request);
            }
        };
        return of(agent, baseChatOp, components);
    }

    private record ProcessorImpl<R>(
            ChatAgent agent,
            ChatRequest request,
            Function<ChatRequest, CompletionStage<R>> operator
    ) implements Component.Processor<R> {

        @Override
        public CompletionStage<R> process(ChatRequest request) {
            return operator.apply(request);
        }

    }

}
