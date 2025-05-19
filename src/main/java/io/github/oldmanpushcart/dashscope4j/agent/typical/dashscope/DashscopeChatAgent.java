package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

/**
 * DashScope 智能体
 */
public class DashscopeChatAgent extends BaseChatAgent {

    private DashscopeChatAgent(Builder builder) {
        super(builder);
    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        final ChatRequest newRequest = newDashscopeChatRequest(request);
        return client().chat().async(newRequest);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        final ChatRequest newRequest = newDashscopeChatRequest(request);
        return client().chat().flow(newRequest);
    }

    private ChatRequest newDashscopeChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(DashscopeChatAgent agent) {
        return new Builder(agent);
    }

    public static class Builder extends BaseChatAgent.Builder<DashscopeChatAgent, Builder> {

        public Builder() {

        }

        public Builder(DashscopeChatAgent agent) {
            super(agent);
        }

        @Override
        public DashscopeChatAgent build() {
            return new DashscopeChatAgent(this);
        }

    }

}
