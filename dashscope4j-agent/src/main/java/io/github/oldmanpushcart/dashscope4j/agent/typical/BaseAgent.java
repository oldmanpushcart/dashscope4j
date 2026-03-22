package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public class BaseAgent implements Agent {

    private final String name;
    private final String description;
    private final String introduction;

    private final DashscopeClient client;
    private final ChatModel model;
    private final Map<String, Object> parameters;
    private final List<Interceptor> interceptors;

    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.introduction = builder.introduction;
        this.client = builder.client;
        this.model = builder.model;
        this.parameters = CommonUtils.unmodifiableCopy(builder.parameters);
        this.interceptors = CommonUtils.unmodifiableCopy(builder.interceptors);
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String introduction() {
        return introduction;
    }

    private AigcRequest<ChatModel.Input, ChatModel.Output> newRequest(UserMessage inbound) {
        return AigcRequest.newBuilder(model())
                .input(ChatModel.Input.newBuilder()
                        .messages(messages -> {

                            // 如果有设置 introduction 则添加
                            final var introduction = introduction();
                            if (CommonUtils.isNotBlankString(introduction)) {
                                final var content = TextContent.newBuilder()
                                        .text(introduction)
                                        .cacheControl(Content.CacheControl.EPHEMERAL)
                                        .build();
                                messages.add(0, Message.system(content));
                            }

                            // 添加用户输入
                            messages.add(inbound);

                            return messages;
                        })
                        .failOnToolError(false)
                        .build())
                .parameters(parameters())
                .build();
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage inbound) {
        final var request = newRequest(inbound);
        return client().async(request, interceptors())
                .thenApply(response -> response.output().best().message());
    }

    @Override
    public Publisher<AssistantMessage> flow(UserMessage inbound) {
        final var request = AigcRequest.newBuilder(newRequest(inbound))
                .parameters(parameters -> {

                    // 如果没有制定输出模式，默认为增量输出
                    parameters.putIfAbsent("incremental_output", true);

                    return parameters;
                })
                .build();
        return Flux.from(client().flow(request, interceptors()))
                .map(response -> response.output().best().message());
    }

    protected DashscopeClient client() {
        return client;
    }

    protected ChatModel model() {
        return model;
    }

    protected Map<String, Object> parameters() {
        return parameters;
    }

    protected List<Interceptor> interceptors() {
        return interceptors;
    }

    public static abstract class Builder<T extends BaseAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String description;
        private String introduction;

        private DashscopeClient client;
        private ChatModel model;
        private Map<String, Object> parameters;
        private List<Interceptor> interceptors;

        protected Builder() {

        }

        protected Builder(BaseAgent agent) {

            this.name = agent.name;
            this.description = agent.description;
            this.introduction = agent.introduction;

            this.client = agent.client;
            this.model = agent.model;
            this.parameters = CommonUtils.unmodifiableCopy(agent.parameters);
            this.interceptors = CommonUtils.unmodifiableCopy(agent.interceptors);

        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B description(String description) {
            this.description = description;
            return self();
        }

        public B introduction(String introduction) {
            this.introduction = introduction;
            return self();
        }

        public B client(DashscopeClient client) {
            this.client = client;
            return self();
        }

        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        public B parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return self();
        }

        public B parameters(UnaryOperator<Map<String, Object>> operator) {
            this.parameters = operator.apply(CommonUtils.mutableCopy(this.parameters));
            return self();
        }

        public B interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return self();
        }

        public B interceptors(UnaryOperator<List<Interceptor>> operator) {
            this.interceptors = operator.apply(CommonUtils.mutableCopy(this.interceptors));
            return self();
        }

    }

}
