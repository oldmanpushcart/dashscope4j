package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Value
class BaseChatOp implements ChatOp {

    ChatOp chatOp;
    Plugin plugin;

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        final Plugin.Processor<ChatResponse> processor = new ProcessorImpl<>(request, chatOp::async);
        return plugin.onAsync(processor);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        final Plugin.Processor<Flowable<ChatResponse>> processor = new ProcessorImpl<>(request, chatOp::flow);
        return plugin.onFlow(processor);
    }

    /**
     * 创建对话操作
     *
     * @param chatOp  初始对话操作
     * @param plugins 插件集合
     * @return 对话操作
     */
    public static ChatOp of(ChatOp chatOp, List<Plugin> plugins) {
        final List<Plugin> clones = new ArrayList<>(plugins);
        Collections.reverse(clones);
        ChatOp op = chatOp;
        for (final Plugin chain : clones) {
            op = new BaseChatOp(op, chain);
        }
        return op;
    }

    /**
     * 创建对话操作
     *
     * @param agent   初始对话操作
     * @param plugins 插件集合
     * @return 对话操作
     */
    public static ChatOp of(BaseChatAgent agent, List<Plugin> plugins) {
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
        return of(baseChatOp, plugins);
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    private static class ProcessorImpl<R> implements Plugin.Processor<R> {

        @Getter
        private final ChatRequest request;

        private final Function<ChatRequest, CompletionStage<R>> operator;

        @Override
        public CompletionStage<R> process(ChatRequest request) {
            return operator.apply(request);
        }

    }

}
