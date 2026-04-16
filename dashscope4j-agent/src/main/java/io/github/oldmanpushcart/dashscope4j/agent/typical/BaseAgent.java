package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.session.SessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.CompressSessionManager;
import io.github.oldmanpushcart.dashscope4j.agent.session.store.HashMapSessionStore;
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
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;


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
 * 子类可以通过重写 {@link #baseAsync(AigcRequest)} 和 {@link #baseFlow(AigcRequest)} 方法
 * 来扩展或修改基础的异步和流式请求处理逻辑。
 * </p>
 *
 * @see Agent
 * @see SessionInterceptor
 */
public class BaseAgent implements Agent {

    /**
     * Agent 名称
     */
    private final String name;
    
    /**
     * Agent 描述
     */
    private final String description;
    
    /**
     * Agent 介绍（系统提示词）
     */
    private final String introduction;

    /**
     * 工具箱，用于管理和查找工具
     */
    private final Toolbox toolbox;
    
    /**
     * 搜索工具，用于根据意图动态查找可用工具
     */
    private final Tool searchTools;

    /**
     * 会话管理器，负责会话的生命周期管理
     */
    private final SessionManager sessionManager;

    /**
     * DashScope 客户端
     */
    private final DashscopeClient client;
    
    /**
     * 使用的对话模型
     */
    private final ChatModel model;
    
    /**
     * 模型参数配置
     */
    private final Map<String, Object> parameters;
    
    /**
     * 请求拦截器列表
     */
    private final List<Interceptor> interceptors;

    /**
     * 构造 BaseAgent
     *
     * @param builder 构建器
     */
    protected BaseAgent(Builder<?, ?> builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.introduction = builder.introduction;

        this.toolbox = builder.toolbox;
        this.searchTools = Optional.of(builder.toolbox)
                .map(toolbox -> new SearchToolsFunction(toolbox).asTool())
                .orElse(null);

        // 如果没有指定会话管理器，则默认创建压缩会话管理器
        this.sessionManager = Optional.of(builder.sessionManager)
                .orElseGet(() -> CompressSessionManager.newBuilder()
                        .store(new HashMapSessionStore())
                        .build());

        this.client = builder.client;
        this.model = builder.model;
        // 创建不可变副本，防止外部修改
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

    /**
     * 构建 AIGC 请求
     * <p>
     * 组装完整的请求对象，包括：
     * <ul>
     *     <li>系统提示词（如果设置了 introduction）</li>
     *     <li>用户输入消息</li>
     *     <li>搜索工具（search_tools）</li>
     *     <li>会话拦截器（SessionInterceptor）</li>
     * </ul>
     * </p>
     *
     * @param sessionId 会话ID，用于区分不同的对话会话
     * @param inbound   用户输入消息
     * @return 构建好的 AIGC 请求
     */
    private AigcRequest<Input, Output> newRequest(String sessionId, UserMessage inbound) {
        // 打开会话
        final var session = sessionManager.open(sessionId);
        return AigcRequest.newBuilder(model())

                // 组装对话输入
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

                // 组装算法参数
                .parameters(parameters -> {
                    parameters.putAll(parameters());

                    // 添加 search_tools 工具
                    if (null != searchTools) {
                        //noinspection unchecked
                        final var tools = (List<Tool>) parameters.computeIfAbsent("tools", k -> new ArrayList<>());
                        tools.add(searchTools);
                    }

                    return parameters;
                })

                // 组装拦截器
                .interceptors(interceptors -> {
                    interceptors.addAll(interceptors());
                    interceptors.add(new SessionInterceptor(session));
                    return interceptors;
                })
                .build();
    }

    @Override
    public CompletionStage<AssistantMessage> async(String sessionId, UserMessage inbound) {
        return CompletableFuture.completedStage(newRequest(sessionId, inbound))
                .thenCompose(this::baseAsync)
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
                    .flatMapMany(request -> Flux.from(baseFlow(request)))
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
     * 获取模型参数配置
     *
     * @return 参数映射表
     */
    protected Map<String, Object> parameters() {
        return parameters;
    }

    /**
     * 获取拦截器列表
     *
     * @return 拦截器列表
     */
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
     * 获取会话管理器
     */
    protected SessionManager sessionManager() {
        return sessionManager;
    }

    /**
     * 执行基础异步请求
     * <p>
     * 子类可以重写此方法来扩展或修改异步请求的处理逻辑。
     * </p>
     *
     * @param request AIGC 请求对象
     * @return 异步响应结果
     */
    protected CompletionStage<AigcResponse<Output>> baseAsync(AigcRequest<Input, Output> request) {
        return client().async(request);
    }

    /**
     * 执行基础流式请求
     * <p>
     * 子类可以重写此方法来扩展或修改流式请求的处理逻辑。
     * </p>
     *
     * @param request AIGC 请求对象
     * @return 流式响应发布者
     */
    protected Publisher<AigcResponse<Output>> baseFlow(AigcRequest<Input, Output> request) {
        return client().flow(request);
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
        private String introduction;

        private Toolbox toolbox;
        private SessionManager sessionManager;

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

        public B sessionManager(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
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
