package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 异步MCP智能体
 */
@Slf4j
public class McpChatAgent extends BaseChatAgent {

    private final McpClientTransport transport;
    private final UnaryOperator<McpClient.AsyncSpec> initializer;
    private final Duration reinitializeInterval;
    private final Duration pingInterval;

    private final ScheduledExecutorService scheduler;
    private final boolean isInternalScheduler;

    private final AtomicBoolean shutdownRef = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<McpAsyncClient>> holderRef
            = new AtomicReference<>(new CompletableFuture<>());
    private final AtomicReference<List<? extends FunctionTool>> mcpFunctionToolsRef
            = new AtomicReference<>(new ArrayList<>());

    protected McpChatAgent(Builder builder) {
        super(builder);

        requireNonNull(builder.transport, "transport is required!");
        requireNonNull(builder.initializer, "initializer is required!");
        requireNonNull(builder.reinitializeInterval, "reinitializeInterval is required!");
        requireNonNull(builder.pingInterval, "pingInterval is required!");

        this.transport = builder.transport;
        this.initializer = builder.initializer;
        this.reinitializeInterval = builder.reinitializeInterval;
        this.pingInterval = builder.pingInterval;

        /*
         * 创建调度器
         *
         * 如果外部有指定则使用外部，否则内部自己创建。
         * 自己创建的调度器将由自己进行生命周期管理
         */
        if (Objects.isNull(builder.scheduler)) {
            this.isInternalScheduler = true;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r ->
                    new Thread(r) {{
                        setName("%s/mcp/scheduler".formatted(McpChatAgent.this.name()));
                        setDaemon(true);
                    }});
        } else {
            this.isInternalScheduler = false;
            this.scheduler = builder.scheduler;
        }

        /*
         * 启动调度
         *
         * 1. 初始化客户端
         * 2. 维持客户端心跳
         *
         * 如初始化、心跳失败，都会回到初始化逻辑
         */
        try {
            initialize(Duration.ZERO);
        } catch (RuntimeException ex) {
            shutdownSchedulerIfNecessary();
            throw ex;
        }

    }

    @Override
    protected CompletionStage<ChatResponse> baseAsync(ChatRequest request) {
        return client().chat().async(request);
    }

    @Override
    protected CompletionStage<Flowable<ChatResponse>> baseFlow(ChatRequest request) {
        return client().chat().flow(request);
    }

    @Override
    protected List<FunctionTool> baseFunctionTools() {
        return new ArrayList<>() {{
            addAll(McpChatAgent.super.baseFunctionTools());
            addAll(mcpFunctionToolsRef.get());
        }};
    }

    CompletionStage<McpAsyncClient> fetch() {
        return holderRef.get();
    }

    /**
     * @return 延迟初始化
     */
    public CompletionStage<McpChatAgent> lazy() {
        return fetch().thenApply(mcpClient -> this);
    }

    private synchronized Mono<Void> notifyResourcesChanged(List<McpSchema.Resource> resources) {
        log.debug("{}/mcp/changed/resources size={}", this, resources.size());
        return Mono.empty();
    }

    private synchronized Mono<Void> notifyPromptsChanged(List<McpSchema.Prompt> prompts) {
        log.debug("{}/mcp/changed/prompts size={}", this, prompts.size());
        return Mono.empty();
    }

    private synchronized Mono<Void> notifyToolsChanged(List<McpSchema.Tool> tools) {
        log.debug("{}/mcp/changed/tools size={}", this, tools.size());
        final var newMcpFunctionTools = tools.stream()
                .map(tool -> new McpFunctionTool(this, tool))
                .toList();
        mcpFunctionToolsRef.set(newMcpFunctionTools);
        return Mono.empty();
    }

    private Mono<Void> handleLogging(McpSchema.LoggingMessageNotification loggingMessage) {
        log.debug("{}/mcp/logging/{}/{} {}", this, loggingMessage.level(), loggingMessage.logger(), loggingMessage.data());
        return Mono.empty();
    }

    private Mono<McpSchema.CreateMessageResult> handleSampling(McpSchema.CreateMessageRequest samplingRequest) {
        return null;
    }


    private void initialize(Duration interval) {

        final var mcpClient = initializer.apply(McpClient.async(transport))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .sampling()
                        .build())
                .resourcesChangeConsumer(this::notifyResourcesChanged)
                .promptsChangeConsumer(this::notifyPromptsChanged)
                .toolsChangeConsumer(this::notifyToolsChanged)
                .loggingConsumer(this::handleLogging)
                .sampling(this::handleSampling)
                .build();

        scheduler.schedule(() -> {

            completedStage(null)

                    // 初始化McpClient
                    .thenCompose(unused -> mcpClient.initialize().toFuture())

                    // 初始化Tool
                    .thenCompose(initializeResult -> {
                        if (Objects.isNull(initializeResult.capabilities().tools())) {
                            return completedStage(initializeResult);
                        }
                        return mcpClient.listTools()
                                .map(McpSchema.ListToolsResult::tools)
                                .flatMap(this::notifyToolsChanged)
                                .toFuture()
                                .thenApply(v -> initializeResult);
                    })

                    // 初始化Prompt
                    .thenCompose(initializeResult -> {
                        if (Objects.isNull(initializeResult.capabilities().prompts())) {
                            return completedStage(initializeResult);
                        }
                        return mcpClient.listPrompts()
                                .map(McpSchema.ListPromptsResult::prompts)
                                .flatMap(this::notifyPromptsChanged)
                                .toFuture()
                                .thenApply(v -> initializeResult);
                    })

                    // 初始化Resources
                    .thenCompose(initializeResult -> {
                        if (Objects.isNull(initializeResult.capabilities().resources())) {
                            return completedStage(initializeResult);
                        }
                        return mcpClient.listResources()
                                .map(McpSchema.ListResourcesResult::resources)
                                .flatMap(this::notifyResourcesChanged)
                                .toFuture()
                                .thenApply(v -> initializeResult);
                    })

                    /*
                     * 初始化成功
                     *
                     * 将初始化好的客户端注入到注册信息中，这样可以通知到所有等待获取客户端的请求拿到最新的客户端
                     * 并创建心跳检测任务检测客户端健康状态
                     */
                    .thenAccept(r -> {
                        log.debug("{} initialized.", this);
                        holderRef.get().complete(mcpClient);
                        heartbeat();
                    })

                    /*
                     * 初始化失败
                     * 销毁临时创建的客户端
                     * 重新创建初始化任务，继续初始化，直到成功或关闭
                     */
                    .exceptionally(ex -> {
                        log.debug("{} initialize failed!", this, ex);
                        if (mcpClient.isInitialized()) {
                            mcpClient.close();
                        }
                        initialize(reinitializeInterval);
                        return null;
                    });

        }, interval.toMillis(), TimeUnit.MILLISECONDS);

    }

    private void heartbeat() {
        scheduler.schedule(() -> {

            final var holder = holderRef.get();
            holder

                    // 发送Ping包做心跳检测
                    .thenCompose(client -> client.ping().toFuture())

                    /*
                     * 心跳检测成功
                     * 说明客户端健康，需要重新创建下一次心跳任务
                     */
                    .thenAccept(r -> {
                        log.debug("{}/{} heartbeat.", this, name());
                        heartbeat();
                    })

                    /*
                     * 心跳检测失败
                     * 说明客户端网络已中断，需要创建重连任务
                     */
                    .exceptionally(ex -> {

                        /*
                         * CAS创建新的客户端持有者
                         * 1. 创建成功后注册重新连任务，重连时会将初始化好的客户端重新注入到新的持有者中。
                         * 2. 对于现有持有者的客户端进行销毁
                         */
                        if (holderRef.compareAndSet(holder, new CompletableFuture<>())) {
                            log.debug("{}/{} heartbeat failed!", this, name(), ex);
                            holder.thenAccept(McpAsyncClient::close);
                            initialize(Duration.ZERO);
                        }
                        return null;

                    });

        }, pingInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void shutdownSchedulerIfNecessary() {
        if (isInternalScheduler) {
            scheduler.shutdownNow();
        }
    }

    /**
     * 销毁客户端注册信息
     * <ul>
     *     <li>若客户端正在初始化，则取消初始化过程</li>
     *     <li>若客户端已经完成初始化，则销毁客户端</li>
     * </ul>
     */
    private void closeMcpClientIfNecessary() {
        final var future = holderRef.get();
        if (!future.cancel(true)
            && future.isDone()
            && !future.isCompletedExceptionally()) {
            future.thenAccept(McpAsyncClient::close);
        }
    }

    /**
     * 关闭
     */
    public void shutdown() {
        if (!shutdownRef.compareAndSet(false, true)) {
            return;
        }
        shutdownSchedulerIfNecessary();
        closeMcpClientIfNecessary();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<McpChatAgent, Builder> {

        private ScheduledExecutorService scheduler;
        private McpClientTransport transport;
        private UnaryOperator<McpClient.AsyncSpec> initializer = v -> v;
        private Duration reinitializeInterval = Duration.ofSeconds(5);
        private Duration pingInterval = Duration.ofSeconds(30);
        private boolean lazy = false;

        /**
         * 设置调度器
         * <p>
         * 调度器将被用于McpClient内部维持客户端状态
         * </p>
         *
         * @param scheduler 调度器
         * @return this
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = requireNonNull(scheduler);
            return this;
        }

        /**
         * 设置McpClientTransport
         * <p>
         * 将决定智能体访问Mcp服务器的网络传输方式
         * </p>
         *
         * @param transport mcp client transport
         * @return this
         */
        public Builder transport(McpClientTransport transport) {
            this.transport = requireNonNull(transport);
            return this;
        }

        /**
         * 设置McpClient初始化
         * <p>
         * 将可以改变McpClient的初始化设置
         * </p>
         *
         * @param initializer 初始化设置
         * @return this
         */
        public Builder initializer(UnaryOperator<McpClient.AsyncSpec> initializer) {
            this.initializer = requireNonNull(initializer);
            return this;
        }

        /**
         * 设置重新初始化间隔
         * <p>
         * 当网络中断或者初始化失败时，将会触发智能体重建McpClient。
         * 这个参数将可以设置每次重新初始化的时间间隔
         * </p>
         *
         * @param reinitializeInterval 重新初始化间隔
         * @return this
         */
        public Builder reinitializeInterval(Duration reinitializeInterval) {
            this.reinitializeInterval = requireNonNull(reinitializeInterval);
            return this;
        }

        /**
         * 设置心跳间隔
         * <p>
         * 当智能体初始化完成后会进入心跳维持阶段，
         * 这个参数可以控制智能体心跳的时间间隔
         * </p>
         *
         * @param pingInterval 心跳时间间隔
         * @return this
         */
        public Builder pingInterval(Duration pingInterval) {
            this.pingInterval = requireNonNull(pingInterval);
            return this;
        }

        /**
         * 设置是否延迟初始化
         *
         * @param lazy 是否延迟初始化
         * @return this
         */
        public Builder lazy(boolean lazy) {
            this.lazy = lazy;
            return this;
        }

        @Override
        public McpChatAgent build() {
            final var agent = new McpChatAgent(this);
            return lazy
                    ? agent.lazy().toCompletableFuture().join()
                    : agent;
        }

    }

}
