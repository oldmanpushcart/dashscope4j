package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

public class BaseAgent implements Agent {

    private final String name;
    private final String description;
    private final String introduction;

    private final Toolbox toolbox;
    private final Tool searchTools;

    private final String sessionId;
    private final Memory memory;

    private final DashscopeClient client;
    private final ChatModel model;
    private final Map<String, Object> parameters;
    private final List<Interceptor> interceptors;

    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.introduction = builder.introduction;

        this.toolbox = builder.toolbox;
        this.searchTools = FunctionTool.newBuilder()
                .name("search_tools")
                .description("根据意图搜索工具。当你没有工具可以完成任务时调用。")
                .parameterType(Search.class)
                .<Search>function((caller, search) -> this.toolbox.lookup(Message.user(search.intent())))
                .build();

        this.sessionId = builder.sessionId;
        this.memory = builder.memory;

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

    private AigcRequest<Input, Output> newRequest(UserMessage inbound) {
        return AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
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
                .parameters(parameters -> {
                    parameters.put("tools", List.of(searchTools));
                    parameters.putAll(parameters());
                    return parameters;
                })
                .interceptors(interceptors -> {
                    interceptors.addAll(interceptors());
                    interceptors.add(new MemoryInterceptor(sessionId, memory));
                    return interceptors;
                })
                .build();
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage inbound) {
        return CompletableFuture.completedStage(newRequest(inbound))
                .thenCompose(this::baseAsync)
                .thenApply(response -> response.output().best().message());
    }

    @Override
    public Publisher<AssistantMessage> flow(UserMessage inbound) {
        // 使用 Flux.defer 延迟执行，在订阅时才进行记忆召回
        return Flux.defer(() -> {

            final var stage = CompletableFuture.completedStage(newRequest(inbound))

                    // 如果没有制定输出模式，默认为增量输出
                    .thenApply(request -> AigcRequest.newBuilder(newRequest(inbound))
                            .parameters(parameters -> {
                                parameters.putIfAbsent("incremental_output", true);
                                return parameters;
                            })
                            .build());

            // 执行记忆召回，然后创建流
            return Mono.fromCompletionStage(stage)
                    .flatMapMany(request -> Flux.from(baseFlow(request)))
                    .map(response -> response.output().best().message());

        });
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(toolbox);
        IOUtils.closeQuietly(memory);
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

    /**
     * 工具箱
     */
    protected Toolbox toolbox() {
        return toolbox;
    }

    /**
     * 内存
     */
    protected Memory memory() {
        return memory;
    }

    /**
     * 基础异步请求
     */
    protected CompletionStage<AigcResponse<Output>> baseAsync(AigcRequest<Input, Output> request) {
        return client().async(request);
    }

    /**
     * 基础流式请求
     */
    protected Publisher<AigcResponse<Output>> baseFlow(AigcRequest<Input, Output> request) {
        return client().flow(request);
    }

    /**
     * 搜索工具
     */
    private record Search(

            @JsonPropertyDescription("意图")
            @JsonProperty("intent")
            String intent

    ) {

    }

    public static abstract class Builder<T extends BaseAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String description;
        private String introduction;

        private Toolbox toolbox;
        private String sessionId;
        private Memory memory;

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

        public B toolbox(Toolbox toolbox) {
            this.toolbox = toolbox;
            return self();
        }

        public B sessionId(String sessionId) {
            this.sessionId = sessionId;
            return self();
        }

        public B memory(Memory memory) {
            this.memory = memory;
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
