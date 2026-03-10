package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Enhancer;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public interface Agent {

    String name();

    String description();

    DashscopeClient client();

    CompletionStage<AssistantMessage> async(UserMessage message);

    Publisher<AssistantMessage> flow(UserMessage message);

    static Builder newBuilder() {
        return null;
    }

    static Builder newBuilder(Agent agent) {
        return null;
    }

    interface Builder extends Buildable<Agent, Builder> {

        Builder name(String name);

        Builder description(String description);

        Builder introduction(String introduction);

        Builder client(DashscopeClient client);

        Builder model(ChatModel model);

        Builder enhancers(List<Enhancer> enhancers);

        Builder enhancers(UnaryOperator<List<Enhancer>> operator);

        Builder interceptors(List<Interceptor> interceptors);

        Builder interceptors(UnaryOperator<List<Interceptor>> operator);

        CompletionStage<Agent> buildAsync();

        @Override
        default Agent build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

    }

}
