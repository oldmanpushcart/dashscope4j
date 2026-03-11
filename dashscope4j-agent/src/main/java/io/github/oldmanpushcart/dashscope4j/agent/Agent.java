package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.enhancer.Enhancer;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public class Agent {

    private final String name;
    private final String description;
    private final DashscopeClient client;

    private final String introduction;
    private final String sessionId;
    private final ChatModel model;
    private final List<Interceptor> interceptors;
    private final List<Enhancer> enhancers;

    private Agent(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.client = builder.client;
        this.introduction = builder.introduction;
        this.sessionId = builder.sessionId;
        this.model = builder.model;
        this.interceptors = builder.interceptors;
        this.enhancers = builder.enhancers;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public DashscopeClient client() {
        return client;
    }

    public String sessionId() {
        return sessionId;
    }

    public CompletionStage<AssistantMessage> async(UserMessage message) {
        final var request = newRequest(message);
        return client().async(request, interceptors)
                .thenApply(response -> response.output().best().message());
    }

    public Publisher<AssistantMessage> flow(UserMessage message) {
        final var request = newRequest(message);

        /*
         * flow 中统一用增量输出
         */
        final var flowRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {
                    parameters.put("incremental_output", true);
                    return parameters;
                })
                .build();

        return Flux.from(client().flow(flowRequest, interceptors))
                .map(response -> response.output().best().message());
    }

    private AigcRequest<Input, Output> newRequest(UserMessage message) {
        return AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .building(builder -> {

                            if (null != introduction && !introduction.isBlank()) {
                                builder.addMessage(Message.system(introduction));
                            }

                        })
                        .addMessage(message)
                        .failOnToolError(false)
                        .build())
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(Agent agent) {
        return new Builder(agent);
    }

    public static class Builder implements Buildable<Agent, Builder> {

        private String name;
        private String description;
        private DashscopeClient client;
        private String introduction;
        private String sessionId;
        private ChatModel model;
        private final List<Interceptor> interceptors = new ArrayList<>();
        private final List<Enhancer> enhancers = new ArrayList<>();

        public Builder() {

        }

        public Builder(Agent agent) {
            this.name = agent.name;
            this.description = agent.description;
            this.client = agent.client;
            this.sessionId = agent.sessionId;
            this.introduction = agent.introduction;
            this.model = agent.model;
            this.interceptors.addAll(agent.interceptors);
            this.enhancers.addAll(agent.enhancers);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        public Builder introduction(String introduction) {
            this.introduction = introduction;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        public Agent.Builder enhancers(List<Enhancer> enhancers) {
            this.enhancers.clear();
            if (null != enhancers) {
                this.enhancers.addAll(enhancers);
            }
            return this;
        }

        public Agent.Builder enhancers(UnaryOperator<List<Enhancer>> operator) {
            return enhancers(operator.apply(this.enhancers));
        }

        public Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors.clear();
            if (null != interceptors) {
                this.interceptors.addAll(interceptors);
            }
            return this;
        }

        public Agent.Builder interceptors(UnaryOperator<List<Interceptor>> operator) {
            return interceptors(operator.apply(this.interceptors));
        }

        public CompletionStage<Agent> buildAsync() {
            final var agent = new Agent(this);
            CompletionStage<Agent> stage = CompletableFuture.completedFuture(agent);
            for (Enhancer enhancer : enhancers) {
                stage = stage.thenCompose(enhancer::enhance);
            }
            return stage;
        }

        @Override
        public Agent build() {
            return buildAsync().toCompletableFuture().join();
        }

    }

}
