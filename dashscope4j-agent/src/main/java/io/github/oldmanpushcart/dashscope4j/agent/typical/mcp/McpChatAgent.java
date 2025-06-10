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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final UnaryOperator<McpClient.AsyncSpec> connector;
    private final ReconnectStrategy reconnectStrategy;
    private final Duration heartbeatInterval;
    private final int heartbeatFailureThreshold;

    private final ScheduledExecutorService scheduler;
    private final boolean isInternalScheduler;

    private final AtomicBoolean shutdownRef = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Hold>> holderRef
            = new AtomicReference<>(new CompletableFuture<>());
    private final AtomicReference<List<? extends FunctionTool>> mcpFunctionToolsRef
            = new AtomicReference<>(new ArrayList<>());

    protected McpChatAgent(Builder builder) {
        super(builder);

        requireNonNull(builder.transport, "transport is required!");
        requireNonNull(builder.connector, "connector is required!");
        requireNonNull(builder.reconnectStrategy, "reconnectStrategy is required!");
        requireNonNull(builder.heartbeatInterval, "heartbeatInterval is required!");

        this.transport = builder.transport;
        this.connector = builder.connector;
        this.reconnectStrategy = builder.reconnectStrategy;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.heartbeatFailureThreshold = builder.heartbeatFailureThreshold;

        /*
         * 创建调度器
         *
         * 如果外部有指定则使用外部，否则内部自己创建。
         * 自己创建的调度器将由自己进行生命周期管理
         */
        if (Objects.nonNull(builder.scheduler)) {
            this.isInternalScheduler = false;
            this.scheduler = builder.scheduler;
        } else {
            this.isInternalScheduler = true;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r ->
                    new Thread(r) {{
                        setName("%s/mcp/scheduler".formatted(McpChatAgent.this.name()));
                        setDaemon(true);
                    }});
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
            schedulingConnectNow();
        } catch (RuntimeException ex) {
            shuttingSchedulerIfNecessary();
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
    protected List<FunctionTool> functionTools() {
        return new ArrayList<>() {{
            addAll(McpChatAgent.super.functionTools());
            addAll(mcpFunctionToolsRef.get());
        }};
    }

    CompletableFuture<Hold> holder() {
        return holderRef.get();
    }

    private boolean tryResetHolder(CompletableFuture<Hold> holder) {
        if (!holderRef.compareAndSet(holder, new CompletableFuture<>())) {
            return false;
        }
        if (!holder.cancel(true)) {
            holder.thenAccept(Hold::closeGracefully);
        }
        return true;
    }

    /**
     * @return 延迟初始化
     */
    public CompletionStage<McpChatAgent> lazy() {
        return holder().thenApply(mcpClient -> this);
    }

    private synchronized Mono<Void> notifyToolsChanged(List<McpSchema.Tool> tools) {
        log.debug("{}/mcp/changed/tools size={}", this, tools.size());
        final var newMcpFunctionTools = tools.stream()
                .map(tool -> new McpFunctionTool(this, tool))
                .toList();
        mcpFunctionToolsRef.set(newMcpFunctionTools);
        return Mono.empty();
    }

    /**
     * 调度一个任务执行，根据指定的延迟时间决定是立即执行还是延迟执行。
     *
     * <p>如果提供的延迟时间为 {@code null} 或零，则任务会立即提交到线程池中执行；否则，任务将在指定的延迟时间后执行。</p>
     *
     * @param task  需要调度执行的任务
     * @param delay 任务执行前的延迟时间；若为 null 或等于零，则任务立即执行
     * @return 调度任务
     * @throws RejectedExecutionException 如果任务无法被调度（例如线程池已关闭或资源不足）
     */
    @SuppressWarnings("UnusedReturnValue")
    private Future<?> scheduling(Runnable task, Duration delay) {
        return Objects.isNull(delay) || delay.isZero()
                ? scheduler.submit(task)
                : scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 调度{@link McpAsyncClient}连接任务，立即执行
     */
    private void schedulingConnectNow() {
        schedulingConnect(0, null);
    }

    /**
     * 调度{@link McpAsyncClient}连接任务，支持失败重试
     *
     * @param attemptCount 当前重试次数
     * @param failureCause 上次失败原因
     */
    private void schedulingConnect(int attemptCount, Throwable failureCause) {

        final var connectInterval = attemptCount == 0
                ? Duration.ZERO
                : reconnectStrategy.retryDelay(attemptCount, failureCause);

        final var mcpClient = connector.apply(McpClient.async(transport))
                .toolsChangeConsumer(this::notifyToolsChanged)
                .build();

        scheduling(() -> completedStage(null)

                // 初始化McpClient
                .thenCompose(unused -> mcpClient.initialize().toFuture())

                /*
                 * 初始化Tools通知
                 * 1. 如果初始化结果中表明支持Tools变动通知，则主动进行一次初始化通知
                 */
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

                /*
                 * 初始化成功
                 * 1. 将初始化好的客户端注入到注册信息中，这样可以通知到所有等待获取客户端的请求拿到最新的客户端
                 * 2. 创建心跳检测任务检测客户端健康状态
                 */
                .thenAccept(r -> {
                    log.debug("{} connected.", this);
                    holderRef.get().complete(new Hold(mcpClient));
                    schedulingHeartbeat();
                })

                /*
                 * 初始化失败
                 * 1. 销毁临时创建的客户端
                 * 2. 重新创建初始化任务，继续初始化，直到成功或关闭
                 */
                .exceptionally(ex -> {
                    log.debug("{} connect failed!", this, ex);
                    if (mcpClient.isInitialized()) {
                        mcpClient.close();
                    }
                    schedulingConnect(attemptCount + 1, ex);
                    return null;
                }), connectInterval);

    }


    /**
     * 调度{@link McpAsyncClient}心跳任务，
     * 支持心跳失败重链
     */
    private void schedulingHeartbeat() {
        scheduling(() -> {
            final var holder = holder();

            holder.thenCompose(hold -> hold.heartbeat()

                    /*
                     * 心跳检测成功
                     * 说明客户端健康，需要重新创建下一次心跳任务
                     */
                    .thenAccept(r -> {
                        log.debug("{}/{} heartbeat.", this, name());
                        hold.notifyHeartbeatSuccess();
                        schedulingHeartbeat();
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
                            hold.notifyHeartbeatFailure(() -> {
                                if (tryResetHolder(holder)) {
                                    schedulingConnectNow();
                                }
                            });
                        }
                        return null;

                    })
            );
        }, heartbeatInterval);
    }


    /**
     * 关闭调度器
     */
    private void shuttingSchedulerIfNecessary() {
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
    private void shuttingMcpClientIfNecessary() {
        final var holder = holderRef.get();
        if (!holder.cancel(true)
                && holder.isDone()) {
            holder.thenAccept(Hold::closeGracefully);
        }
    }

    /**
     * 关闭
     */
    public void shutdown() {
        if (!shutdownRef.compareAndSet(false, true)) {
            return;
        }
        shuttingSchedulerIfNecessary();
        shuttingMcpClientIfNecessary();
    }

    @AllArgsConstructor
    class Hold {

        @Getter
        @Accessors(fluent = true)
        private final McpAsyncClient client;
        private final AtomicInteger heartbeatFailures = new AtomicInteger();

        @SuppressWarnings("UnusedReturnValue")
        public CompletionStage<?> closeGracefully() {
            return client.closeGracefully().toFuture();
        }

        public CompletionStage<?> heartbeat() {
            return client.ping().toFuture();
        }

        public void notifyHeartbeatSuccess() {
            heartbeatFailures.set(0);
        }

        public void notifyHeartbeatFailure(Runnable trigger) {
            final int failures = heartbeatFailures.incrementAndGet();
            final boolean shouldTrigger = heartbeatFailureThreshold <= 0 || failures > heartbeatFailureThreshold;
            if (shouldTrigger) {
                trigger.run();
            }
        }

    }


    /**
     * 重连策略
     */
    public interface ReconnectStrategy {

        /**
         * 重连延迟
         *
         * @param attemptCount 当前重连次数
         * @param failureCause 上次失败原因
         * @return 重连延迟
         */
        Duration retryDelay(int attemptCount, Throwable failureCause);

    }

    /**
     * 重连策略工厂
     */
    public interface ReconnectStrategies {

        /**
         * 指数退避重连策略
         *
         * @param baseDelay   初始延迟
         * @param maxDelay    最大延迟
         * @param jitterRatio 抖动比例
         * @return 指数重连策略
         */
        static ReconnectStrategy exponentialBackoff(final Duration baseDelay, final Duration maxDelay, final double jitterRatio) {
            final Random random = new Random();
            return (attemptCount, failureCause) -> {

                if (attemptCount <= 0) {
                    return Duration.ZERO;
                }

                // exponential backoff: baseDelay * 2^(attemptCount - 1)
                final double expBackoffMillis = baseDelay.toMillis() * Math.pow(2, attemptCount - 1);

                // apply jitter: ± jitterRatio of the current exponential delay
                final double jitter = (random.nextDouble() * 2 - 1) * jitterRatio * expBackoffMillis;

                final long calculatedDelay = (long) (expBackoffMillis + jitter);

                // cap at maxDelay
                return Duration.ofMillis(Math.min(calculatedDelay, maxDelay.toMillis()));
            };

        }

    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseChatAgent.Builder<McpChatAgent, Builder> {

        private ScheduledExecutorService scheduler;
        private McpClientTransport transport;
        private UnaryOperator<McpClient.AsyncSpec> connector = v -> v;
        private ReconnectStrategy reconnectStrategy = ReconnectStrategies.exponentialBackoff(
                Duration.ofSeconds(1),
                Duration.ofMinutes(5),
                0.5
        );
        private Duration heartbeatInterval = Duration.ofSeconds(30);
        private int heartbeatFailureThreshold;
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
            this.connector = requireNonNull(initializer);
            return this;
        }

        /**
         * 设置重连策略
         *
         * @param reconnectStrategy 重连策略
         * @return this
         */
        public Builder reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = requireNonNull(reconnectStrategy);
            return this;
        }

        /**
         * 设置心跳间隔
         * <p>
         * 当智能体初始化完成后会进入心跳维持阶段，
         * 这个参数可以控制智能体心跳的时间间隔
         * </p>
         *
         * @param heartbeatInterval 心跳时间间隔
         * @return this
         */
        public Builder heartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = requireNonNull(heartbeatInterval);
            return this;
        }


        /**
         * 设置心跳失败阈值
         *
         * @param heartbeatFailureThreshold 心跳失败阈值
         * @return this
         */
        public Builder heartbeatFailureThreshold(int heartbeatFailureThreshold) {
            this.heartbeatFailureThreshold = heartbeatFailureThreshold;
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
