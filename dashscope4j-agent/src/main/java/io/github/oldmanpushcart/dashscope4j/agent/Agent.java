package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.hook.Hook;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public interface Agent {

    String name();

    String description();

    DashscopeClient client();

    CompletionStage<AssistantMessage> async(UserMessage message);

    Publisher<AssistantMessage> flow(UserMessage message);

    interface Builder extends Buildable<Agent, Builder> {

        Builder name(String name);

        Builder description(String description);

        Builder client(DashscopeClient client);

        Builder introduction(String introduction);

        Builder hooks(List<Hook> hooks);

        Builder hooks(UnaryOperator<List<Hook>> operator);

        Builder model(ChatModel model);

        Builder parameters(Map<String, Object> parameters);

        Builder parameters(UnaryOperator<Map<String, Object>> operator);

        Builder interceptors(List<Interceptor> interceptors);

        Builder interceptors(UnaryOperator<List<Interceptor>> operator);

        Builder tools(List<Tool> tools);

        Builder tools(UnaryOperator<List<Tool>> operator);

        CompletionStage<Agent> buildAsync();

        @Override
        default Agent build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

    }

}
