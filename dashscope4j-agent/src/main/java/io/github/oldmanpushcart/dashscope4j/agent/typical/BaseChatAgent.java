package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.common.util.flow.FlowX;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public abstract class BaseChatAgent extends BaseAgent implements ChatAgent {

    private final String introduction;
    private final ChatModel model;
    private final Parameters parameters;
    private final List<Interceptor> interceptors;

    protected BaseChatAgent(Builder<?, ?> builder) {
        super(builder);
        this.introduction = builder.introduction;
        this.model = builder.model;
        this.parameters = builder.parameters;
        this.interceptors = builder.interceptors;
    }

    protected String introduction() {
        return introduction;
    }

    protected ChatModel model() {
        return model;
    }

    protected Parameters parameters() {
        return parameters;
    }

    protected List<Interceptor> interceptors() {
        return interceptors;
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage message) {
        final var request = newRequest(message);
        return client().async(request, interceptors())
                .thenApply(response -> response.output().best().message());
    }

    @Override
    public Flow.Publisher<AssistantMessage> flow(UserMessage message) {
        final var request = newRequest(message);
        return FlowX.fromPublisher(client().flow(request, interceptors()))
                .map(response -> response.output().best().message());
    }

    private AigcRequest<Input, Output> newRequest(UserMessage message) {
        return AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .building(builder -> {

                            final var introduction = introduction();
                            if (null != introduction && !introduction.isBlank()) {
                                builder.addMessage(Message.system(introduction));
                            }

                        })
                        .addMessage(message)
                        .failOnToolError(false)
                        .build())
                .building(builder -> {

                    final var parameters = parameters();
                    if (null != parameters && !parameters.isEmpty()) {
                        builder.parameters(parameters);
                    }

                })
                .addParameter("parallel_tool_calls", false)
                .build();
    }

    public static abstract class Builder<A extends BaseChatAgent, B extends BaseChatAgent.Builder<A, B>>
            extends BaseAgent.Builder<A, B>
            implements Buildable<A, B> {

        private String introduction;
        private ChatModel model;
        private Parameters parameters;
        private List<Interceptor> interceptors;

        public B introduction(String introduction) {
            this.introduction = introduction;
            return self();
        }

        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        public B parameters(Parameters parameters) {
            this.parameters = parameters;
            return self();
        }

        public B interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return self();
        }

    }

}
