package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.PublisherUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.Objects.*;
import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * 可恢复的 MCP 客户端传输层包装器
 * <p>
 * 提供自动重连能力的 MCP 传输层封装，核心特性包括：
 * <ul>
 *     <li><b>自动重连</b>：连接断开后自动重试，支持指数退避策略</li>
 *     <li><b>心跳检测</b>：定期发送 Ping 消息检测连接健康状态</li>
 *     <li><b>故障计数</b>：跟踪连续发送失败和心跳失败次数</li>
 *     <li><b>优雅关闭</b>：支持资源的有序释放和清理</li>
 * </ul>
 * </p>
 * <p>
 * 当检测到网络故障时（发送失败或心跳超时），会自动重置连接并触发重连流程。
 * 重连间隔采用指数退避算法，避免对服务器造成过大压力。
 * </p>
 *
 * @see McpClientTransport
 * @see ReconnectStrategy
 */
public class RecoverableMcpClientTransport implements McpClientTransport {

    /**
     * 组件名称
     */
    private static final String NAME = "mcp-client-transport/recoverable";
    
    /**
     * 默认心跳间隔：30 秒
     */
    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofSeconds(30);
    
    /**
     * 默认心跳超时时间：60 秒（心跳间隔的 2 倍）
     */
    private static final Duration DEFAULT_PING_TIMEOUT = Duration.ofSeconds(60); // 2x ping interval
    
    /**
     * 默认最大连续发送失败次数
     */
    private static final int DEFAULT_MAX_CONSECUTIVE_SEND_FAILURES = 5;
    
    /**
     * 默认最大连续心跳失败次数
     */
    private static final int DEFAULT_MAX_CONSECUTIVE_PING_FAILURES = 5;
    
    /**
     * 最大重试次数：防止指数退避计算溢出
     */
    private static final int MAX_RETRY_ATTEMPT = 63; // Prevent overflow in exponential backoff
    
    /**
     * 默认重连策略：指数退避
     */
    private static final ReconnectStrategy DEFAULT_RECONNECT_STRATEGY = ReconnectStrategies
            .exponentialBackoff(
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(1),
                    0.3
            );

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * JSON 映射器
     */
    private final McpJsonMapper mapper;
    
    /**
     * 传输层工厂，用于创建底层传输实例
     */
    private final McpClientTransportFactory transportFactory;
    
    /**
     * 重连策略
     */
    private final ReconnectStrategy reconnectStrategy;
    
    /**
     * 最大连续发送失败次数阈值
     */
    private final int maxConsecutiveSendFailures;

    /**
     * 是否拥有调度器的所有权
     */
    private final boolean ownsScheduler;
    
    /**
     * 调度器，用于执行定时任务
     */
    private final ScheduledExecutorService scheduler;

    /**
     * 是否启用心跳检测
     */
    private final boolean pingEnabled;
    
    /**
     * 心跳间隔时间
     */
    private final Duration pingInterval;
    
    /**
     * 心跳超时时间
     */
    private final Duration pingTimeout; // Timeout for each ping request
    
    /**
     * 最大连续心跳失败次数阈值
     */
    private final int maxConsecutivePingFailures;

    /**
     * 关闭标志
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    /**
     * 当前持有的连接（包含传输层和消息处理器）
     */
    private final AtomicReference<CompletableFuture<Hold>> holderRef = new AtomicReference<>(new CompletableFuture<>());
    
    /**
     * 心跳检测器
     */
    private final Pinger pinger = new Pinger();
    
    /**
     * 网络健康状态跟踪器
     */
    private final NetworkHealth networkHealth = new NetworkHealth();
    
    // Cache the closing future to handle duplicate close requests
    private volatile CompletableFuture<Void> closingFuture;

    private RecoverableMcpClientTransport(Builder builder) {

        requireNonNull(builder.transportFactory, "transportFactory must not be null.");
        
        // Validate parameters
        if (builder.maxConsecutiveSendFailures < 0) {
            throw new IllegalArgumentException("maxConsecutiveSendFailures must be non-negative");
        }
        if (builder.pingInterval != null && builder.pingInterval.toMillis() <= 0) {
            throw new IllegalArgumentException("pingInterval must be positive");
        }
        if (builder.pingTimeout != null && builder.pingTimeout.toMillis() <= 0) {
            throw new IllegalArgumentException("pingTimeout must be positive");
        }

        this.mapper = requireNonNullElseGet(builder.mapper, () -> new JacksonMcpJsonMapper(JacksonJsonUtils.newMapper()));
        this.transportFactory = builder.transportFactory;
        this.reconnectStrategy = requireNonNullElse(builder.reconnectStrategy, DEFAULT_RECONNECT_STRATEGY);
        this.maxConsecutiveSendFailures = builder.maxConsecutiveSendFailures;

        // Configure pinger
        this.pingEnabled = builder.pingEnabled;
        this.pingInterval = requireNonNullElse(builder.pingInterval, DEFAULT_PING_INTERVAL);
        this.pingTimeout = requireNonNullElse(builder.pingTimeout, DEFAULT_PING_TIMEOUT);
        this.maxConsecutivePingFailures = builder.maxConsecutivePingFailures;
        
        // Validate pingTimeout >= pingInterval
        if (this.pingTimeout.compareTo(this.pingInterval) < 0) {
            throw new IllegalArgumentException("pingTimeout must be >= pingInterval");
        }

        // Create scheduler if necessary
        if (Objects.isNull(builder.scheduler)) {
            this.ownsScheduler = true;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                final Thread thread = new Thread(r);
                thread.setName("%s/scheduler".formatted(NAME));
                thread.setDaemon(true);
                return thread;
            });
        } else {
            this.ownsScheduler = false;
            this.scheduler = builder.scheduler;
        }

    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        schedulingConnectNow(pinger.wrapHandler(handler::apply));
        return Mono.empty();
    }

    @Override
    public Mono<Void> closeGracefully() {

        // Do nothing if the transport is already closed or closing
        if (!closed.compareAndSet(false, true)) {
            // Return cached closing future if available, otherwise empty mono
            final var cachedClosing = closingFuture;
            return cachedClosing != null 
                    ? Mono.from(PublisherUtils.unwrapCancellableStage(cachedClosing))
                    : Mono.empty();
        }

        // main closing
        final var closingF = CompletableFuture.completedStage(null)

                // closing pinger
                .thenAccept(unused -> pinger.close())

                // closing holder
                .thenCompose(unused -> closingHolder())
                .exceptionally(ex -> {
                    logger.warn("{} closing holder failed.", this, ex);
                    return null;
                })

                // closing scheduler
                .thenCompose(unused -> closingSchedulerIfNecessary())
                .exceptionally(ex -> {
                    logger.warn("{} closing scheduler failed.", this, ex);
                    return null;
                })

                // finally closed
                .thenAccept(unused -> logger.info("{} closed.", this));
        
        // Cache the closing future for duplicate close requests
        this.closingFuture = closingF.toCompletableFuture();

        return Mono.from(PublisherUtils.unwrapCancellableStage(closingF));
    }


    /**
     * Closing the holder
     */
    private CompletionStage<Void> closingHolder() {
        final var holder = getHolder();
        if (!holder.cancel(true) && holder.isDone()) {
            return holder.thenCompose(hold -> hold.closeGracefully().toFuture())
                    .exceptionally(ex -> {
                        logger.warn("{} closing holder's transport failed.", this, ex);
                        return null;
                    });
        }
        return completedStage(null);
    }

    /*
     * Closing the scheduler
     */
    private CompletionStage<Void> closingSchedulerIfNecessary() {
        if (!ownsScheduler) {
            return completedStage(null);
        }
        final var closingF = new CompletableFuture<Void>();
        try {
            logger.info("{} shutting down scheduler.", this);
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    closingF.completeExceptionally(new IllegalStateException("Failed to close scheduler!"));
                } else {
                    logger.warn("{} scheduler shutdown with force.", this);
                    closingF.complete(null);
                }
            } else {
                logger.info("{} scheduler shutdown gracefully.", this);
                closingF.complete(null);
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
            closingF.completeExceptionally(ex);
        }
        return closingF;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

        // Skip sending if the transport has already been closed
        if (isClosed()) {
            return Mono.empty();
        }

        // Get the current holder
        final var holder = getHolder();

        // Send the message and handle result
        final var sendF = holder.thenCompose(hold ->

                // Perform the actual message sending
                hold.transport.sendMessage(message)
                        .toFuture()

                        // On success: reset consecutive failure count
                        .thenAccept(unused -> networkHealth.notifySendSuccess())

                        // On failure: handle send error
                        .exceptionallyCompose(ex -> {
                            logger.warn("{}/send failed. message={}, cause={}", 
                                    this, message.getClass().getSimpleName(), ex.toString());
                            networkHealth.notifySendFailure(() -> {
                                if (tryResetHolder(holder)) {
                                    schedulingConnectNow(hold.handler);
                                }
                            });
                            return CompletableFuture.failedStage(ex);
                        }))
                
                // Handle case where holder was cancelled or completed during send
                .exceptionallyCompose(ex -> {
                    if (ex instanceof CancellationException) {
                        logger.debug("{}/send cancelled. holder was reset during send.", this);
                    } else {
                        logger.warn("{}/send failed with exception. cause={}", this, ex.toString());
                    }
                    return CompletableFuture.failedStage(ex);
                });

        return Mono.fromFuture(sendF);
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return mapper.convertValue(data, typeRef);
    }

    private boolean isClosed() {
        return closed.get();
    }


    /**
     * 获取当前持有的连接
     *
     * @return 连接 Future
     */
    private CompletableFuture<Hold> getHolder() {
        return holderRef.get();
    }

    /**
     * 尝试重置连接持有者
     * <p>
     * 使用 CAS 操作原子性地替换旧的连接持有者为新的持有者，
     * 并异步关闭旧的连接。
     * </p>
     *
     * @param holder 旧的连接持有者
     * @return true 如果成功重置，false 如果 CAS 失败
     */
    private boolean tryResetHolder(CompletableFuture<Hold> holder) {
        // Create new holder first to minimize race condition window
        final var newHolder = new CompletableFuture<Hold>();
        if (!holderRef.compareAndSet(holder, newHolder)) {
            return false;
        }
        
        // Close the old holder asynchronously
        closeOldHolder(holder);
        return true;
    }

    /**
     * 优雅关闭旧的连接持有者
     *
     * @param holder 旧的连接持有者
     */
    private void closeOldHolder(CompletableFuture<Hold> holder) {
        if (!holder.cancel(true)) {
            // Holder is already done, close it gracefully
            holder.thenAccept(hold -> {
                hold.closeGracefully().subscribe(
                    unused -> logger.debug("{} closed old holder's transport.", this),
                    ex -> logger.warn("{} failed to close old holder's transport.", this, ex)
                );
            });
        } else {
            logger.debug("{} cancelled old holder.", this);
        }
    }

    /**
     * 调度任务执行
     *
     * @param task  要执行的任务
     * @param delay 延迟时间，如果为 null 或零则立即执行
     * @return 调度任务的 Future
     */
    private Future<?> scheduling(Runnable task, Duration delay) {
        try {
            return Objects.isNull(delay) || delay.isZero()
                    ? scheduler.submit(task)
                    : scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            // Scheduler is shutting down, ignore the task
            logger.debug("{} rejected task execution. scheduler is shutting down.", this);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 重连上下文
     *
     * @param handler      消息处理器
     * @param attemptCount 重试次数
     * @param failureCause 失败原因
     */
    private record ReconnectContext(Handler handler, int attemptCount, Throwable failureCause) {
        
        /**
         * 创建下一次重试的上下文
         *
         * @param cause 失败原因
         * @return 新的重连上下文
         */
        ReconnectContext nextAttempt(Throwable cause) {
            return new ReconnectContext(handler, attemptCount + 1, cause);
        }
    }

    /**
     * 立即调度连接任务
     *
     * @param handler 消息处理器
     * @return 调度任务的 Future
     */
    private Future<?> schedulingConnectNow(Handler handler) {
        return schedulingConnect(new ReconnectContext(handler, 0, null));
    }


    /**
     * 调度带重试的连接任务
     * <p>
     * 根据重试次数计算连接间隔，首次连接或立即重连时隔阂为零。
     * 连接成功后启动心跳检测，连接失败则调度下一次重试。
     * </p>
     *
     * @param context 重连上下文
     * @return 调度任务的 Future
     */
    private Future<?> schedulingConnect(ReconnectContext context) {

        /*
         * Compute the connection interval based on retry attempt count.
         *
         * If attemptCount is 0, it indicates the first connection or an immediate reconnect,
         * in which case the interval is set to zero.
         */
        final var connectInterval = context.attemptCount() == 0
                ? Duration.ZERO
                : reconnectStrategy.retryDelay(context.attemptCount(), context.failureCause());

        /*
         * schedule connect
         *
         * if successful, then schedule heartbeat
         * else schedule reconnect
         */
        return scheduling(() -> completedStage(null)
                .thenApply(unused -> transportFactory.create(mapper))

                // Connect transport
                .thenCompose(transport -> transport.connect(context.handler())
                        .toFuture()
                        .thenApply(unused -> transport))

                /*
                 * Connect transport success
                 *
                 * complete the holder instance and schedule heartbeat,
                 * if complete failed, close the transport and cleanup resources.
                 */
                .thenAccept(transport -> {
                    if (getHolder().complete(new Hold(transport, context.handler()))) {
                        logger.info("{} connected successfully. scheduling heartbeat.", this);
                        // Reset network health counters after successful connection
                        networkHealth.reset();
                        schedulingPing();
                    } else {
                        // Holder was reset during connection, cleanup transport
                        logger.debug("{} holder was reset during connection, closing transport.", this);
                        transport.close();
                    }
                })

                // Connect transport failed, schedule reconnect with connect interval
                .exceptionally(ex -> {
                    logger.warn("{} connect failed, scheduling reconnect. attempt={}, cause={}", 
                            this, context.attemptCount() + 1, ex.toString());
                    schedulingConnect(context.nextAttempt(ex));
                    return null;
                }), connectInterval);

    }


    /**
     * Schedules a ping task to periodically check the connection health.
     *
     * <p>If the ping succeeds, it resets the consecutive failure counter
     * and schedules the next ping.</p>
     *
     * <p>If the ping fails beyond the configured threshold,
     * the holder will be reset and a reconnection attempt is triggered.</p>
     *
     * @see #pinger
     * @see #tryResetHolder(CompletableFuture)
     * @see #schedulingConnectNow(Handler)
     */
    private void schedulingPing() {

        // Skip if ping is disabled
        if (!pingEnabled) {
            return;
        }

        scheduling(() -> {
            final var holder = getHolder();
            holder.thenCompose(hold -> pinger.ping(hold)

                    /*
                     * Heartbeat success
                     *
                     * 1. reset consecutive failure counter
                     * 2. continue to schedule heartbeat
                     */
                    .thenAccept(unused -> {
                        networkHealth.notifyPingSuccess();
                        // Only schedule next ping if this holder is still active
                        if (holder.isDone() && !holder.isCompletedExceptionally() && !holder.isCancelled()) {
                            logger.trace("{} heartbeat success, scheduling next ping.", this);
                            schedulingPing();
                        } else {
                            logger.debug("{} heartbeat success but holder changed, skipping next ping.", this);
                        }
                    })

                    /*
                     * Heartbeat failed
                     *
                     * 1. increment consecutive failure counter
                     * 2. if consecutive failure count exceeds the threshold, reset the holder and schedule reconnect
                     * 3. stop scheduling new pings (reconnection will handle it)
                     */
                    .exceptionallyCompose(ex -> {
                        networkHealth.notifyPingFailure(() -> {
                            if (tryResetHolder(holder)) {
                                schedulingConnectNow(hold.handler);
                            }
                        });
                        // Don't continue scheduling on failure - let reconnection handle it
                        return null;
                    }));

        }, pingInterval);
    }


    /**
     * 连接持有者
     * <p>
     * 封装了底层的 MCP 传输层和消息处理器。
     * </p>
     *
     * @param transport 底层传输层
     * @param handler   消息处理器
     */
        private record Hold(McpClientTransport transport, Handler handler) {

        /**
             * 优雅关闭传输层
             *
             * @return 关闭完成的 Mono
             */
            public Mono<Void> closeGracefully() {
                return transport.closeGracefully();
            }

        }

    /**
     * 网络健康状态跟踪器
     * <p>
     * 集中管理所有故障计数和重连逻辑，避免在 Hold 实例中重复管理状态。
     * 跟踪连续发送失败和心跳失败次数，当超过阈值时触发重连。
     * </p>
     */
    private class NetworkHealth {

        /**
         * 连续发送失败次数
         */
        private final AtomicInteger consecutiveSendFailures = new AtomicInteger(0);
        
        /**
         * 连续心跳失败次数
         */
        private final AtomicInteger consecutivePingFailures = new AtomicInteger(0);
        
        /**
         * 是否正在重连
         */
        private final AtomicBoolean reconnecting = new AtomicBoolean(false);

        /**
         * 通知发送操作成功
         * <p>
         * 重置连续发送失败计数器。
         * </p>
         */
        public void notifySendSuccess() {
            consecutiveSendFailures.set(0);
        }

        /**
         * 通知发送操作失败
         * <p>
         * 如果失败次数超过阈值，则触发重连。
         * </p>
         *
         * @param trigger 重连触发回调
         */
        public void notifySendFailure(Runnable trigger) {
            final int failures = consecutiveSendFailures.incrementAndGet();
            // Cap the failure count to prevent integer overflow
            if (failures < 0) {
                consecutiveSendFailures.set(Integer.MAX_VALUE - 1);
            }
            final boolean shouldTrigger = maxConsecutiveSendFailures <= 0 || failures > maxConsecutiveSendFailures;
            if (shouldTrigger && reconnecting.compareAndSet(false, true)) {
                trigger.run();
            }
        }

        /**
         * 通知心跳成功
         * <p>
         * 重置连续心跳失败计数器和重连标志。
         * </p>
         */
        public void notifyPingSuccess() {
            consecutivePingFailures.set(0);
            reconnecting.set(false); // Reset reconnect flag on successful ping
        }

        /**
         * 通知心跳失败
         * <p>
         * 如果失败次数超过阈值，则触发重连。
         * </p>
         *
         * @param trigger 重连触发回调
         */
        public void notifyPingFailure(Runnable trigger) {
            final int failures = consecutivePingFailures.incrementAndGet();
            // Cap the failure count to prevent integer overflow
            if (failures < 0) {
                consecutivePingFailures.set(Integer.MAX_VALUE - 1);
            }
            final boolean shouldTrigger = maxConsecutivePingFailures <= 0 || failures > maxConsecutivePingFailures;
            if (shouldTrigger && reconnecting.compareAndSet(false, true)) {
                trigger.run();
            }
        }

        /**
         * 重置所有计数器
         * <p>
         * 在成功重连后调用。
         * </p>
         */
        public void reset() {
            consecutiveSendFailures.set(0);
            consecutivePingFailures.set(0);
            reconnecting.set(false);
        }

    }


    /**
     * MCP 客户端传输层工厂接口
     */
    public interface McpClientTransportFactory {

        /**
         * 创建新的 MCP 客户端传输层实例
         *
         * @param mapper JSON 映射器
         * @return MCP 客户端传输层实例
         */
        McpClientTransport create(McpJsonMapper mapper);

    }

    /**
     * 重连策略接口
     */
    public interface ReconnectStrategy {

        /**
         * 根据重试次数和失败原因计算重试延迟
         *
         * @param attemptCount 重试次数
         * @param failureCause 失败原因
         * @return 重试延迟时间
         */
        Duration retryDelay(int attemptCount, Throwable failureCause);

    }

    public interface ReconnectStrategies {

        /**
         * Create a new strategy.
         *
         * @param baseDelay   initial delay (base)
         * @param maxDelay    maximum allowed delay
         * @param jitterRatio randomness ratio to avoid thundering herd problem
         */
        static ReconnectStrategy exponentialBackoff(final Duration baseDelay, final Duration maxDelay, final double jitterRatio) {
            final Random random = new Random();
            return (attemptCount, failureCause) -> {

                if (attemptCount <= 0) {
                    return Duration.ZERO;
                }

                // Cap attempt count to prevent overflow in exponential calculation
                final int cappedAttemptCount = Math.min(attemptCount, MAX_RETRY_ATTEMPT);

                // exponential backoff: baseDelay * 2^(attemptCount - 1)
                final double expBackoffMillis = baseDelay.toMillis() * Math.pow(2, cappedAttemptCount - 1);

                // apply jitter: ± jitterRatio of the current exponential delay
                final double jitter = (random.nextDouble() * 2 - 1) * jitterRatio * expBackoffMillis;

                final long calculatedDelay = (long) (expBackoffMillis + jitter);

                // cap at maxDelay and ensure non-negative
                return Duration.ofMillis(Math.max(0, Math.min(calculatedDelay, maxDelay.toMillis())));
            };

        }

    }


    /**
     * Handler interface for handling messages.
     */
    private interface Handler extends Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> {

    }

    /**
     * 心跳检测器
     * <p>
     * 负责监控连接健康状态，通过定期发送 Ping 消息并等待 Pong 响应来检测连接是否正常。
     * 使用会话映射表跟踪待处理的 Ping 请求，支持超时处理和容量限制。
     * </p>
     */
    private class Pinger implements AutoCloseable {

        /**
         * Ping 消息前缀，用于区分 Ping 请求和普通请求
         */
        private static final String PREFIX = "HB-PING";
        
        /**
         * 最大会话数，防止内存耗尽
         */
        // Limit session map size to prevent memory exhaustion
        private static final int MAX_SESSIONS = 1000;
        
        /**
         * 会话映射表：Ping ID -> Pong 回调 Future
         */
        private final Map<String, CompletableFuture<Void>> sessionMap = new ConcurrentHashMap<>();

        /**
         * 向服务器发送 Ping 并等待 Pong 响应
         *
         * @param hold 连接持有者
         * @return 收到 Pong 时完成的 CompletionStage
         * @throws IllegalStateException 如果会话映射表已满
         */
        public CompletionStage<?> ping(Hold hold) {

            /*
             * Ping 请求标识
             *
             * 格式：PREFIX-UUID
             * 需要特殊前缀来区分 Ping 请求和普通请求。
             */
            final var pingId = "%s-%s".formatted(PREFIX, UUID.randomUUID().toString());

            // Pong 回调 Future
            final var pongF = new CompletableFuture<Void>();

            /*
             * 检查会话映射表容量，防止内存耗尽
             */
            if (sessionMap.size() >= MAX_SESSIONS) {
                logger.warn("{} session map full ({} sessions), rejecting ping.", this, MAX_SESSIONS);
                pongF.completeExceptionally(new IllegalStateException("Session map capacity exceeded"));
                return pongF;
            }

            /*
             * Ping 超时任务
             *
             * 如果在指定超时时间内未收到 Pong，
             * 将移除 Ping 会话并取消 Pong 回调 Future。
             */
            final var timeoutF = schedulingPingTimeout(pingId, pongF);

            /*
             * 注册到会话
             *
             * 如果 pingId 匹配，将完成 Pong 回调 Future。
             */
            sessionMap.put(pingId, pongF);

            /*
             * 发送 Ping 请求
             *
             * 如果发送失败，将从 sessionMap 中移除 Pong 回调 Future，
             * 并取消 Ping 超时任务。
             */
            return hold.transport.sendMessage(new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_PING, pingId, null))
                    .toFuture()
                    .thenCompose(unused -> pongF)
                    .whenComplete((r, ex) -> {
                        // Clean up session map entry
                        sessionMap.remove(pingId, pongF);
                        if (!timeoutF.isDone()) {
                            timeoutF.cancel(true);
                        }
                    });
        }

        /**
         * 调度 Ping 超时任务
         *
         * @param pingId Ping ID
         * @param pongF  Pong 回调 Future
         * @return Ping 超时 Future
         */
        private Future<?> schedulingPingTimeout(String pingId, CompletableFuture<Void> pongF) {
            return scheduling(() -> {
                if (pongF.cancel(true)) {
                    logger.warn("{}/heartbeat/{} timeout!", RecoverableMcpClientTransport.this, pingId);
                }
            }, pingTimeout); // Use dedicated timeout, not interval
        }

        /**
         * 检查指定的 requestId 是否为 Ping ID
         *
         * @param requestId 请求 ID
         * @return 如果是 Ping ID 则返回 true，否则返回 false
         */
        private boolean isPingId(String requestId) {
            return requestId.startsWith(PREFIX);
        }

        /**
         * 包装处理器以拦截 Ping 响应
         * <p>
         * 该方法在传输层初始化期间调用，用于设置消息过滤器。
         * 当收到 Ping 响应时，完成对应的 Future 并从会话映射表中移除。
         * </p>
         *
         * @param delegate 原始处理器
         * @return 包装后的处理器，会过滤 Ping 响应
         */
        public Handler wrapHandler(Handler delegate) {
            return mono -> mono
                    .filter(message -> {

                        /*
                         * When received a message, check if it is a ping response.
                         * If it is a ping response, remove the corresponding ping session and complete the pong callback future.
                         *
                         * Ping response's id must be a pingId, which is a special prefix + UUID.
                         */
                        if (message instanceof McpSchema.JSONRPCResponse response
                                && response.id() instanceof String requestId
                                && isPingId(requestId)) {
                            final var pongF = sessionMap.remove(requestId);
                            if (null != pongF) {
                                // Complete only if not already cancelled
                                if (!pongF.isCancelled()) {
                                    pongF.complete(null);
                                }
                            }
                            return false;
                        }
                        return true;
                    })
                    .transform(delegate);
        }

        @Override
        public void close() {
            sessionMap.forEach((pingId, pongF) -> pongF.cancel(true));
            sessionMap.clear();
        }

    }

    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * RecoverableMcpClientTransport 构建器
     * <p>
     * 使用 Builder 模式配置可恢复的 MCP 传输层。
     * </p>
     */
    public static class Builder implements Buildable<RecoverableMcpClientTransport, Builder> {

        /**
         * JSON 映射器
         */
        private McpJsonMapper mapper;
        
        /**
         * 传输层工厂
         */
        private McpClientTransportFactory transportFactory;
        
        /**
         * 重连策略
         */
        private ReconnectStrategy reconnectStrategy;
        
        /**
         * 调度器
         */
        private ScheduledExecutorService scheduler;
        
        /**
         * 最大连续发送失败次数，默认为 5
         */
        private int maxConsecutiveSendFailures = DEFAULT_MAX_CONSECUTIVE_SEND_FAILURES;

        /**
         * 是否启用心跳检测，默认启用
         */
        private boolean pingEnabled = true;
        
        /**
         * 心跳间隔时间，默认为 30 秒
         */
        private Duration pingInterval = DEFAULT_PING_INTERVAL;
        
        /**
         * 心跳超时时间，默认为 60 秒
         */
        private Duration pingTimeout = DEFAULT_PING_TIMEOUT;
        
        /**
         * 最大连续心跳失败次数，默认为 5
         */
        private int maxConsecutivePingFailures = DEFAULT_MAX_CONSECUTIVE_PING_FAILURES;


        /**
         * 设置 JSON 映射器
         *
         * @param mapper JSON 映射器
         * @return 当前构建器
         */
        public Builder mapper(McpJsonMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * 设置传输层工厂
         *
         * @param transportFactory 传输层工厂
         * @return 当前构建器
         */
        public Builder transportFactory(McpClientTransportFactory transportFactory) {
            this.transportFactory = transportFactory;
            return this;
        }

        /**
         * 设置重连策略
         *
         * @param reconnectStrategy 重连策略
         * @return 当前构建器
         */
        public Builder reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = reconnectStrategy;
            return this;
        }

        /**
         * 设置调度器
         *
         * @param scheduler 调度器
         * @return 当前构建器
         */
        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * 设置最大连续发送失败次数
         *
         * @param maxConsecutiveSendFailures 最大连续发送失败次数
         * @return 当前构建器
         */
        public Builder maxConsecutiveSendFailures(int maxConsecutiveSendFailures) {
            this.maxConsecutiveSendFailures = maxConsecutiveSendFailures;
            return this;
        }

        /**
         * 设置心跳间隔时间
         *
         * @param pingInterval 心跳间隔
         * @return 当前构建器
         */
        public Builder pingInterval(Duration pingInterval) {
            this.pingInterval = pingInterval;
            return this;
        }

        /**
         * 设置心跳超时时间
         *
         * @param pingTimeout 心跳超时时间
         * @return 当前构建器
         */
        public Builder pingTimeout(Duration pingTimeout) {
            this.pingTimeout = pingTimeout;
            return this;
        }

        /**
         * 设置最大连续心跳失败次数
         *
         * @param maxConsecutivePingFailures 最大连续心跳失败次数
         * @return 当前构建器
         */
        public Builder maxConsecutivePingFailures(int maxConsecutivePingFailures) {
            this.maxConsecutivePingFailures = maxConsecutivePingFailures;
            return this;
        }

        /**
         * 设置是否启用心跳检测
         *
         * @param pingEnabled 是否启用
         * @return 当前构建器
         */
        public Builder pingEnabled(boolean pingEnabled) {
            this.pingEnabled = pingEnabled;
            return this;
        }

        /**
         * 构建可恢复的 MCP 传输层
         *
         * @return 新创建的传输层实例
         */
        @Override
        public RecoverableMcpClientTransport build() {
            return new RecoverableMcpClientTransport(this);
        }

    }

}
