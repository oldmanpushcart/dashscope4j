package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * 抽象的智能体实现
 */
@Slf4j
@Accessors(fluent = true)
public abstract class BaseChatAgent implements ChatAgent {

    private static final AtomicInteger identityGen = new AtomicInteger(100);

    @Getter
    private final String name;

    @Getter
    private final String summary;

    @Getter(AccessLevel.PROTECTED)
    private final DashscopeClient client;

    private final ChatModel model;
    private final boolean flowBridge;
    private final List<Interceptor> interceptors;

    @Getter(AccessLevel.PROTECTED)
    private final List<ChatFunctionTool> functionTools;

    private final List<Plugin> plugins;
    private final ChatOp chatOp;

    protected BaseChatAgent(Builder<?, ?> builder) {

        requireNonNull(builder.client, "client is required!");

        this.name = buildingName(builder.name);
        this.summary = builder.summary;
        this.client = builder.client;
        this.model = builder.model;
        this.flowBridge = builder.flowBridge;
        this.interceptors = unmodifiableList(builder.interceptors);
        this.functionTools = unmodifiableList(builder.functionTools);
        this.plugins = unmodifiableList(builder.plugins);
        this.chatOp = newChatOp(this, plugins);

    }

    /*
     * 创建新的对话操作，在这个新的对话操作中
     * 1. 对话原有的async/flow将会被baseAsync/baseFlow所取代
     * 2. 代理智能体的Plugin
     */
    private static ChatOp newChatOp(BaseChatAgent agent,  List<Plugin> plugins) {
        final List<Plugin> merged = new ArrayList<>(plugins);
        merged.add(new BaseRewriteUserMessagePlugin());
        return BaseChatOp.of(agent, merged);
    }

    private static String buildingName(String name) {
        return StringUtils.isNotBlank(name)
                ? name
                : String.format("chat-agent-%s", identityGen.incrementAndGet());
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedFuture(request)

                // 创建新的对话请求
                .thenApply(this::newChatRequest)

                // 根据流控开关选择不同的执行方式
                .thenCompose(newRequest -> flowBridge
                        ? asyncByFlowBridge(newRequest)
                        : chatOp.async(newRequest))

                // 记录日志
                .whenComplete((r, ex) ->
                        log.debug("dashscope-agent://{}/async completed.", name(), ex))
                ;
    }

    // 流式桥接异步
    private CompletionStage<ChatResponse> asyncByFlowBridge(ChatRequest request) {

        // 强制开启增量输出模式
        final ChatRequest newRequest = ChatRequest.newBuilder(request)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        // 将增量流式输出的ChatResponse合并为一个ChatResponse，并返回
        return chatOp.flow(newRequest)
                .thenCompose(responseFlow ->
                        responseFlow
                                .reduce(ChatResponse::accumulate)
                                .toCompletionStage());
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest request) {
        return CompletableFuture.completedFuture(request)
                .thenApply(this::newChatRequest)
                .thenCompose(chatOp::flow)
                .whenComplete((r, ex) ->
                        log.debug("dashscope-agent://{}/flow completed.", name(), ex));
    }

    private ChatRequest newChatRequest(ChatRequest request) {
        return ChatRequest.newBuilder(request)
                .building(builder -> ofNullable(model).ifPresent(builder::model))
                .addInterceptors(interceptors)
                .tools(functionTools)
                .build();
    }

    /**
     * 异步对话
     *
     * @param request 对话请求
     * @return 对话结果
     */
    abstract protected CompletionStage<ChatResponse> baseAsync(ChatRequest request);

    /**
     * 流式对话
     *
     * @param request 对话请求
     * @return 对话结果
     */
    abstract protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request);

    @Override
    public ChatAgent.FunctionToolBuilder newFunctionToolBuilder() {
        return new BaseChatAgentFunctionToolBuilder(this);
    }


    /**
     * 抽象智能体功能工具构建器
     *
     * @param <T> 智能体类型
     * @param <B> 构造器类型
     */
    public static abstract class Builder<T extends BaseChatAgent, B extends Builder<T, B>> implements Buildable<T, B> {

        private String name;
        private String summary;
        private DashscopeClient client;
        private ChatModel model;
        private boolean flowBridge;
        private final List<Plugin> plugins = new ArrayList<>();
        private final List<Interceptor> interceptors = new ArrayList<>();
        private final List<ChatFunctionTool> functionTools = new ArrayList<>();

        public Builder() {

        }

        public Builder(BaseChatAgent agent) {
            this.name = agent.name;
            this.summary = agent.summary;
            this.client = agent.client;
            this.model = agent.model;
            this.plugins.addAll(agent.plugins);
            this.interceptors.addAll(agent.interceptors);
            this.functionTools.addAll(agent.functionTools);
        }

        /**
         * 设置智能体名称
         *
         * @param name 智能体名称
         * @return this
         */
        public B name(String name) {
            this.name = requireNonNull(name, "name is required!");
            return self();
        }

        /**
         * 智能体摘要
         *
         * @param summary 智能体摘要
         * @return this
         */
        public B summary(String summary) {
            this.summary = requireNonNull(summary, "summary is required!");
            return self();
        }

        /**
         * 设置 Dashscope4j 客户端
         *
         * @param client Dashscope4j 客户端
         * @return this
         */
        public B client(DashscopeClient client) {
            this.client = requireNonNull(client, "client is required!");
            return self();
        }

        /**
         * 设置对话模型
         *
         * @param model 对话模型
         * @return this
         */
        public B model(ChatModel model) {
            this.model = model;
            return self();
        }

        /**
         * 设置是否启用流式对话桥接
         *
         * @param enabled 是否启用流式对话桥接
         * @return this
         */
        public B flowBridge(boolean enabled) {
            this.flowBridge = enabled;
            return self();
        }

        /**
         * 设置拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B interceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加拦截器
         *
         * @param interceptor 拦截器
         * @return this
         */
        public B addInterceptor(Interceptor interceptor) {
            requireNonNull(interceptor);
            this.interceptors.add(interceptor);
            return self();
        }

        /**
         * 添加拦截器列表
         *
         * @param interceptors 拦截器列表
         * @return this
         */
        public B addInterceptors(List<Interceptor> interceptors) {
            requireNonNull(interceptors);
            this.interceptors.addAll(interceptors);
            return self();
        }

        /**
         * 添加函数工具
         *
         * @param functionTool 函数工具
         * @return this
         */
        public B addFunctionTool(ChatFunctionTool functionTool) {
            this.functionTools.add(functionTool);
            return self();
        }

        /**
         * 批量添加函数工具
         *
         * @param functionTools 函数工具集合
         * @return this
         */
        public B addFunctionTools(Collection<? extends ChatFunctionTool> functionTools) {
            this.functionTools.addAll(functionTools);
            return self();
        }

        /**
         * 设置函数工具列表
         *
         * @param functionTools 函数工具列表
         * @return this
         */
        public B functionTools(Collection<? extends ChatFunctionTool> functionTools) {
            this.functionTools.clear();
            this.functionTools.addAll(functionTools);
            return self();
        }

        /**
         * 添加函数
         *
         * @param function 函数
         * @return this
         */
        public B addFunction(ChatFunction<?, ?> function) {
            return addFunctionTool(ChatFunctionTool.of(function));
        }

        /**
         * 批量添加函数
         *
         * @param functions 函数列表
         * @return this
         */
        public B addFunctions(Collection<? extends ChatFunction<?, ?>> functions) {
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return addFunctionTools(functionTools);
        }

        /**
         * 设置函数列表
         *
         * @param functions 函数列表
         * @return this
         */
        public B functions(Collection<? extends ChatFunction<?, ?>> functions) {
            final List<ChatFunctionTool> functionTools = functions.stream()
                    .map(ChatFunctionTool::of)
                    .collect(Collectors.toList());
            return functionTools(functionTools);
        }

        /**
         * 添加插件
         *
         * @param plugin 插件
         * @return this
         */
        public B addPlugin(Plugin plugin) {
            this.plugins.add(plugin);
            return self();
        }

        /**
         * 批量添加插件
         *
         * @param plugins 插件集合
         * @return this
         */
        public B addPlugins(Collection<? extends Plugin> plugins) {
            this.plugins.addAll(plugins);
            return self();
        }

        /**
         * 设置插件列表
         *
         * @param plugins 插件集合
         * @return this
         */
        public B plugins(Collection<? extends Plugin> plugins) {
            this.plugins.clear();
            this.plugins.addAll(plugins);
            return self();
        }

    }

}
