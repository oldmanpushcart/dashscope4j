package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public abstract class BaseChatAgent extends BaseAgent implements ChatAgent {

    private final String introduction;
    private final ChatModel model;
    private final Map<String, Object> parameters;
    private final List<Interceptor> interceptors;
    private final List<Tool> tools;

    protected BaseChatAgent(Builder<?, ?> builder) {
        super(builder);
        this.introduction = builder.introduction;
        this.model = builder.model;
        this.parameters = builder.parameters;
        this.interceptors = builder.interceptors;
        this.tools = builder.tools;

    }

    protected String introduction() {
        return introduction;
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

    protected List<Tool> tools() {
        return tools;
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage message) {
        final var request = newRequest(message);
        return client().async(request, interceptors())
                .thenApply(response -> response.output().best().message());
    }

    @Override
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

        return Flux.from(client().flow(flowRequest, interceptors()))
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
                .parameters(parameters -> {

                    final var agentParameters = parameters();
                    if (null != agentParameters && !agentParameters.isEmpty()) {
                        parameters.putAll(agentParameters);
                    }

                    final var tools = tools();
                    if (null != tools && !tools.isEmpty()) {
                        parameters.put("tools", tools.toArray(new Tool[0]));
                    }

                    parameters.put("parallel_tool_calls", false);
                    return parameters;
                })
                .build();
    }

    public static abstract class Builder<A extends BaseChatAgent, B extends Builder<A, B>>
            extends BaseAgent.Builder<A, B>
            implements Buildable<A, B> {

        private String introduction;
        private ChatModel model;
        private Map<String,Object> parameters;
        private List<Interceptor> interceptors;
        private List<Tool> tools;

        public B introduction(String introduction) {
            this.introduction = introduction;
            return self();
        }

        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        public B parameters(Map<String,Object> parameters) {
            this.parameters = parameters;
            return self();
        }

        public B interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return self();
        }

        public B tools(List<Tool> tools) {
            this.tools = tools;
            return self();
        }

    }

}
