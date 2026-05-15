package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
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

import static io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin.Phases.INTERACTION;
import static io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin.Phases.PREPARATION;


/**
 * Agent 基础实现类
 * <p>
 * 提供 Agent 的核心功能实现，包括：
 * <ul>
 *     <li>会话管理：支持通过 sessionId 区分不同会话</li>
 *     <li>记忆管理：自动集成 MemoryInterceptor 实现对话历史管理</li>
 *     <li>工具搜索：内置 search_tools 工具，用于动态查找可用工具</li>
 *     <li>请求构建：统一处理消息、参数和拦截器的组装</li>
 * </ul>
 * </p>
 * <p>
 * 来扩展或修改基础的异步和流式请求处理逻辑。
 * </p>
 *
 * @see Agent
 */
public abstract class BaseAgent implements Agent {

    private final String name;
    private final String description;
    private final DashscopeClient client;
    private final ChatModel model;
    private final List<Plugin> plugins;
    private final List<Toolkit> toolkits;

    /**
     * 构造 BaseAgent
     *
     * @param builder 构建器
     */
    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.client = builder.client;
        this.model = builder.model;
        this.plugins = CommonUtils.unmodifiableCopy(builder.plugins);
        this.toolkits = CommonUtils.unmodifiableCopy(builder.toolkits);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    /**
     * 构建 AIGC 请求
     * <p>
     * 组装完整的请求对象，包括：
     * <ul>
     *     <li>系统提示词（如果设置了 introduction）</li>
     *     <li>用户输入消息</li>
     *     <li>搜索工具（search_tools）</li>
     *     <li>会话拦截器（InjectInterceptor）</li>
     * </ul>
     * </p>
     *
     * @param sessionId 会话ID，用于区分不同的对话会话
     * @param inbound   用户输入消息
     * @return 构建好的 AIGC 请求
     */
    private AigcRequest<Input, Output> newRequest(String sessionId, UserMessage inbound) {
        return AigcRequest.newBuilder(model())

                // 组装对话输入
                .input(Input.newBuilder()

                        // 工具调用不失败
                        .failOnToolError(false)

                        // 用户输入
                        .messages(List.of(inbound))

                        // 添加工具
                        .lookups(lookups -> {
                            final var tools = toolkits.stream()
                                    .map(Toolkit::tools)
                                    .flatMap(List::stream)
                                    .toList();
                            final var lookup = ToolLookup.tools(tools);
                            lookups.add(lookup);
                            return lookups;
                        })

                        // 构建
                        .build())

                // 组装拦截器
                .interceptors(interceptors(INTERACTION))

                // 注入会话ID
                .context(Map.of("SESSION-ID", sessionId))

                // 构建请求
                .build();
    }

    private List<Interceptor> interceptors(Plugin.Phases phases) {
        return plugins().stream()
                .map(plugin -> plugin.interceptors(phases))
                .flatMap(List::stream)
                .map(Interceptor.class::cast)
                .toList();
    }

    protected List<Plugin> plugins() {
        return plugins;
    }

    @Override
    public CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound) {
        return CompletableFuture.completedStage(newRequest(sessionId, inbound))
                .thenCompose(request -> client.async(request, interceptors(PREPARATION)))
                .thenApply(response -> response.output().best().message());
    }

    @Override
    public Publisher<AssistantMessage> flow(String sessionId, UserMessage inbound) {
        // 使用 Flux.defer 延迟执行，在订阅时才进行记忆召回
        return Flux.defer(() -> {

            final var stage = CompletableFuture.completedStage(newRequest(sessionId, inbound))

                    // 如果没有制定输出模式，默认为增量输出
                    .thenApply(request -> AigcRequest.newBuilder(request)
                            .parameters(parameters -> {
                                parameters.putIfAbsent("incremental_output", true);
                                return parameters;
                            })
                            .build());

            // 执行记忆召回，然后创建流
            return Mono.fromCompletionStage(stage)
                    .flatMapMany(request -> Flux.from(client.flow(request, interceptors(PREPARATION))))
                    .map(response -> response.output().best().message());

        });
    }

    /**
     * 获取 DashScope 客户端
     *
     * @return DashScope 客户端实例
     */
    protected DashscopeClient client() {
        return client;
    }

    /**
     * 获取对话模型
     *
     * @return 对话模型实例
     */
    protected ChatModel model() {
        return model;
    }

    /**
     * BaseAgent 构建器
     * <p>
     * 使用 Builder 模式构建 BaseAgent 实例，支持链式调用。
     * </p>
     *
     * @param <T> Agent 类型
     * @param <B> Builder 类型
     */
    public static abstract class Builder<T extends BaseAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String description;

        private DashscopeClient client;
        private ChatModel model;

        private List<Plugin> plugins;
        private List<Toolkit> toolkits;


        protected Builder() {

        }

        protected Builder(BaseAgent agent) {

            this.name = agent.name;
            this.description = agent.description;

            this.client = agent.client;
            this.model = agent.model;
            this.plugins = agent.plugins;
            this.toolkits = agent.toolkits;

        }

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B description(String description) {
            this.description = description;
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

        public B plugins(List<Plugin> plugins) {
            this.plugins = plugins;
            return self();
        }

        public B plugins(UnaryOperator<List<Plugin>> operator) {
            this.plugins = operator.apply(CommonUtils.mutableCopy(this.plugins));
            return self();
        }

        public B toolkits(List<Toolkit> toolkits) {
            this.toolkits = toolkits;
            return self();
        }

        public B toolkits(UnaryOperator<List<Toolkit>> operator) {
            this.toolkits = operator.apply(CommonUtils.mutableCopy(this.toolkits));
            return self();
        }

    }

}
