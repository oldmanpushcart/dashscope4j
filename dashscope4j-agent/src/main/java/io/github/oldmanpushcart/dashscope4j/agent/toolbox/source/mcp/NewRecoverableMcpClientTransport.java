package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
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
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNullElseGet;

public class NewRecoverableMcpClientTransport implements McpClientTransport {

    private static final String NAME = "jinx://mcp/recoverable-mcp-client-transport";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpJsonMapper mapper;
    private final ReconnectStrategy reconnectStrategy;
    private final Function<McpJsonMapper, McpClientTransport> transportFactory;
    private final boolean ownsScheduler;

    private final AtomicReference<Hold> holder = new AtomicReference<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();
    private final CompletableFuture<?> connectF = new CompletableFuture<>();

    private volatile ScheduledExecutorService scheduler;
    private volatile Future<?> task;


    // --- PING ---
    private final boolean pingEnabled;
    private final Duration pingInterval;
    private final Duration pingTimeout;
    private final int maxConsecutivePingFailures;
    private final Pinger pinger;

    public NewRecoverableMcpClientTransport(Builder builder) {
        Objects.requireNonNull(builder.reconnectStrategy, "reconnectStrategy must not be null!");
        Objects.requireNonNull(builder.transportFactory, "transportFactory must not be null!");

        this.mapper = requireNonNullElseGet(builder.mapper, () -> new JacksonMcpJsonMapper(JacksonJsonUtils.newMapper()));
        this.reconnectStrategy = builder.reconnectStrategy;
        this.transportFactory = builder.transportFactory;
        this.scheduler = builder.scheduler;
        this.ownsScheduler = null == builder.scheduler;

        this.pingEnabled = builder.pingEnabled;
        this.pingInterval = Objects.requireNonNullElseGet(builder.pingInterval, () -> Duration.ofSeconds(10));
        this.pingTimeout = Objects.requireNonNullElseGet(builder.pingTimeout, () -> Duration.ofSeconds(3));
        this.maxConsecutivePingFailures = Objects.requireNonNullElse(builder.maxConsecutivePingFailures, 3);
        this.pinger = new Pinger();

    }

    private boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public Mono<Void> closeGracefully() {

        if (isClosed() || !closeF.complete(null)) {
            return Mono.empty();
        }

        // 关闭PINGER
        pinger.close();

        // 中断当前任务
        if (null != task && !task.isDone() && !task.isCancelled()) {
            task.cancel(true);
            task = null;
        }

        // 关闭调度器
        synchronized (this) {
            if (ownsScheduler && null != scheduler) {
                scheduler.shutdown();
                scheduler = null;
            }
        }

        final var hold = holder.getAndSet(null);
        return null != hold
                ? hold.transport().closeGracefully()
                : Mono.empty();

    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

        if (isClosed()) {
            return Mono.error(new IllegalStateException("Already closed!"));
        }

        final var hold = holder.get();
        if (null == hold) {
            return Mono.error(new IllegalStateException("Not connected!"));
        }

        return hold.transport().sendMessage(message)
                .doOnError(ex -> {
                    if (holder.compareAndSet(hold, null)) {
                        hold.transport().close();
                        task = schedulingConnectNow(hold.handler());
                    }
                });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return mapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {

        if (isClosed()) {
            return Mono.error(new IllegalStateException("Already closed!"));
        }

        if (!connectF.complete(null)) {
            return Mono.error(new IllegalStateException("Duplicate connected!"));
        }

        task = schedulingConnectNow(pinger.wrapHandler(handler::apply));
        return Mono.empty();
    }


    private Future<?> schedulingConnectNow(Handler handler) {
        return schedulingConnect(new Attempt(handler, 0, null));
    }

    private Future<?> schedulingConnect(Attempt attempt) {

        /*
         * 计算重试间隔
         *
         * 如果是第一次（count == 0）失败，则应该立即重连，即重试间隔为0；
         * 如果是第N次失败，则应该根据重试策略计算重试间隔；
         */
        final var connectInterval = attempt.count() == 0
                ? Duration.ZERO
                : reconnectStrategy.retryDelay(attempt.count(), attempt.cause());

        /*
         * 重试间隔为null，说明策略放弃了本次重试。
         * 如果重试被放弃，则需要立即关闭Transport
         */
        if (null == connectInterval) {
            logger.debug("{} close by reconnect strategy give up!", this);
            close();
            return CompletableFuture.failedFuture(new IllegalStateException("Give up reconnect by error!", attempt.cause()));
        }

        // 记录重试间隔
        if (null != attempt.cause()) {
            logger.warn("{} connect fail, will be retry after {}ms! attempt={}",
                    this,
                    connectInterval.toMillis(),
                    attempt.count(),
                    attempt.cause()
            );
        }

        return scheduling(() -> {
            final var transport = transportFactory.apply(mapper);
            transport.connect(attempt.handler())
                    .doOnSuccess(u -> {
                        logger.debug("{} connect success. attempt={}", this, attempt.count());

                        // 重置HOLDER
                        final var hold = new Hold(transport, attempt.handler());
                        final var exist = holder.getAndSet(hold);
                        if (null != exist) {
                            exist.transport().close();
                        }

                        // 启动PINGER
                        pinger.schedulingPing(hold);

                        // 当前任务完成
                        task = null;

                    })
                    .doOnError(ex -> {
                        transport.close();
                        task = schedulingConnect(attempt.next(ex));
                    })
            ;
        }, connectInterval);

    }

    private Future<?> scheduling(Runnable task, Duration delay) {

        synchronized (this) {

            if (isClosed()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Already closed!"));
            }

            if (ownsScheduler && null == scheduler) {
                scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    final Thread thread = new Thread(r);
                    thread.setName("%s/scheduler".formatted(NAME));
                    thread.setDaemon(true);
                    return thread;
                });
            }

        }

        try {
            return null == delay || delay.isZero()
                    ? scheduler.submit(task)
                    : scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException reEx) {
            return CompletableFuture.failedFuture(reEx);
        }

    }

    @FunctionalInterface
    private interface Handler extends Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> {
    }

    private record Hold(McpClientTransport transport, Handler handler) {

    }

    /**
     * 重连上下文
     *
     * @param handler 消息处理器
     * @param count   重试次数
     * @param cause   失败原因
     */
    private record Attempt(Handler handler, int count, Throwable cause) {

        /**
         * 创建下一次重试的上下文
         *
         * @param cause 失败原因
         * @return 新的重连上下文
         */
        Attempt next(Throwable cause) {
            return new Attempt(handler, count + 1, cause);
        }

    }

    private class Pinger implements AutoCloseable {

        /**
         * Ping 消息前缀，用于区分 Ping 请求和普通请求
         */
        private static final String PREFIX = "HB-PING";

        /**
         * 最大会话数，防止内存耗尽
         */
        private static final int MAX_SESSIONS = 1000;

        /**
         * 会话映射表：Ping ID -> Pong 回调 Future
         */
        private final Map<String, CompletableFuture<Void>> sessionMap = new ConcurrentHashMap<>();

        /**
         * 连续心跳失败次数
         */
        private final AtomicInteger consecutivePingFailures = new AtomicInteger(0);

        public Future<?> schedulingPing(Hold hold) {

            if (!pingEnabled) {
                return CompletableFuture.completedFuture(null);
            }

            return scheduling(() -> {

                ping(hold)
                        .thenAccept(u -> {
                            consecutivePingFailures.set(0);
                            schedulingPing(hold);
                        })
                        .exceptionally(ex -> {
                            final var failures = consecutivePingFailures.incrementAndGet();
                            if (failures >= maxConsecutivePingFailures
                                    && holder.compareAndSet(hold, null)) {
                                schedulingConnectNow(hold.handler());
                            }
                            return null;
                        });

            }, pingInterval);

        }

        /**
         * 向服务器发送 Ping 并等待 Pong 响应
         *
         * @param hold 连接持有者
         * @return 收到 Pong 时完成的 CompletionStage
         * @throws IllegalStateException 如果会话映射表已满
         */
        private CompletionStage<?> ping(Hold hold) {

            /*
             * Ping 请求标识
             *
             * 格式：PREFIX-UUID
             * 需要特殊前缀来区分 Ping 请求和普通请求。
             */
            final var pingId = "%s-%s".formatted(PREFIX, UUID.randomUUID().toString());

            // Pong 回调
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
            return hold.transport().sendMessage(new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_PING, pingId, null))
                    .toFuture()
                    .thenCompose(unused -> pongF)
                    .whenComplete((r, ex) -> {
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
         * @param pongF  Pong 回调
         * @return 调度任务
         */
        private Future<?> schedulingPingTimeout(String pingId, CompletableFuture<Void> pongF) {
            return scheduling(() -> {
                if (pongF.cancel(true)) {
                    logger.warn("{}/heartbeat/{} timeout!", NewRecoverableMcpClientTransport.this, pingId);
                }
            }, pingTimeout);
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
                         * 检查收到的每个消息，它的应答ID是否符合PING的格式。
                         * 如果符合则说明本消息应由PINGER接手。
                         *
                         * 找到ID对应的PONG，并通知其完成。
                         */
                        if (message instanceof McpSchema.JSONRPCResponse response
                                && response.id() instanceof String requestId
                                && isPingId(requestId)) {
                            final var pongF = sessionMap.remove(requestId);
                            if (null != pongF) {
                                pongF.complete(null);
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


    @FunctionalInterface
    public interface ReconnectStrategy {

        Duration retryDelay(int attemptCount, Throwable failureCause);

        default ReconnectStrategy combine(ReconnectStrategy next) {
            return (attemptCount, failureCause) -> {
                final var delay = retryDelay(attemptCount, failureCause);
                final var nextDelay = next.retryDelay(attemptCount, failureCause);
                if (null == delay || null == nextDelay) {
                    return null;
                }
                return Duration.ofMillis(Math.max(delay.toMillis(), nextDelay.toMillis()));
            };
        }

    }

    public interface ReconnectStrategies {

        static ReconnectStrategy never() {
            return (attemptCount, failureCause) -> null;
        }

        static ReconnectStrategy always() {
            return (attemptCount, failureCause) -> Duration.ZERO;
        }

        static ReconnectStrategy max(final int maxRetry) {
            return (attemptCount, failureCause) ->
                    attemptCount < maxRetry
                            ? Duration.ZERO
                            : null;
        }

        static ReconnectStrategy delay(Duration delay) {
            return (attemptCount, failureCause) -> delay;
        }

        static ReconnectStrategy causeBy(Predicate<Throwable> predicate) {
            return (attemptCount, failureCause) ->
                    predicate.test(failureCause)
                            ? Duration.ZERO
                            : null;
        }

        static ReconnectStrategy backoff(final Duration baseDelay, final Duration maxDelay, final double jitterRatio) {
            return (attemptCount, failureCause) -> {

                if (attemptCount <= 0) {
                    return Duration.ZERO;
                }

                // Cap attempt count to prevent overflow in exponential calculation
                final int cappedAttemptCount = Math.min(attemptCount, 63);

                // exponential backoff: baseDelay * 2^(attemptCount - 1)
                final double expBackoffMillis = baseDelay.toMillis() * Math.pow(2, cappedAttemptCount - 1);

                // apply jitter: ± jitterRatio of the current exponential delay
                final double jitter = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * jitterRatio * expBackoffMillis;

                final long calculatedDelay = (long) (expBackoffMillis + jitter);

                // cap at maxDelay and ensure non-negative
                return Duration.ofMillis(Math.max(0, Math.min(calculatedDelay, maxDelay.toMillis())));

            };
        }


    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<NewRecoverableMcpClientTransport, Builder> {

        private McpJsonMapper mapper;
        private ReconnectStrategy reconnectStrategy;
        private Function<McpJsonMapper, McpClientTransport> transportFactory;
        private ScheduledExecutorService scheduler;

        private boolean pingEnabled;
        private Duration pingInterval;
        private Duration pingTimeout;
        private Integer maxConsecutivePingFailures;

        public Builder mapper(McpJsonMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = reconnectStrategy;
            return this;
        }

        public Builder transportFactory(Function<McpJsonMapper, McpClientTransport> transportFactory) {
            this.transportFactory = transportFactory;
            return this;
        }

        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder pingEnabled(boolean pingEnabled) {
            this.pingEnabled = pingEnabled;
            return this;
        }

        public Builder pingInterval(Duration pingInterval) {
            this.pingInterval = pingInterval;
            return this;
        }

        public Builder pingTimeout(Duration pingTimeout) {
            this.pingTimeout = pingTimeout;
            return this;
        }

        public Builder maxConsecutivePingFailures(int maxConsecutivePingFailures) {
            this.maxConsecutivePingFailures = maxConsecutivePingFailures;
            return this;
        }

        @Override
        public NewRecoverableMcpClientTransport build() {
            return new NewRecoverableMcpClientTransport(this);
        }

    }


}
