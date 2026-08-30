package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.mcp;

import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.completedStage;

public class RecoverableMcpClientTransport implements McpClientTransport {

    private static final AtomicInteger SEQUENCER = new AtomicInteger(1000);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String _toString;
    private final McpJsonMapper mapper;
    private final ReconnectStrategy reconnectStrategy;
    private final Function<McpJsonMapper, McpClientTransport> transportFactory;
    private final boolean ownsScheduler;

    private final AtomicReference<CompletableFuture<Hold>> holderRef = new AtomicReference<>(new CompletableFuture<>());
    private final AtomicBoolean connectF = new AtomicBoolean(false);
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private volatile ScheduledExecutorService scheduler;


    // --- PING ---
    private final boolean pingEnabled;
    private final Duration pingInterval;
    private final Duration pingTimeout;
    private final int maxConsecutivePingFailures;
    private final Pinger pinger;

    public RecoverableMcpClientTransport(Builder builder) {
        CheckUtils.requireNonBlankString(builder.name, "name must not be blank!");
        Objects.requireNonNull(builder.reconnectStrategy, "reconnectStrategy must not be null!");
        Objects.requireNonNull(builder.transportFactory, "transportFactory must not be null!");

        this._toString = "dashscope4j-agent://mcp/recoverable-transport/%s@%d".formatted(builder.name, SEQUENCER.incrementAndGet());
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

    /**
     * @return 是否已关闭
     */
    private boolean isClosed() {
        return closeF.isDone();
    }

    /**
     * @return 获取连接持有者
     */
    private CompletableFuture<Hold> getHolder() {
        return holderRef.get();
    }

    /**
     * 尝试重置连接持有者。
     * <p>
     * 仅当持有者当前持有的连接实例与期望值一致时，才会将其替换为一个新的空连接实例，
     * 表示当前连接已被废弃，等待新连接来填充。
     * </p>
     *
     * @param expect 期待连接持有者所拥有的连接实例
     * @return TRUE | FALSE
     */
    private boolean tryResetHolder(CompletableFuture<Hold> expect) {
        return holderRef.compareAndSet(expect, new CompletableFuture<>());
    }

    /**
     * 关闭连接持有者
     *
     * @return 关闭回调
     */
    private CompletionStage<Void> closingHolder() {
        final var holder = getHolder();

        // 成功取消：transport 从未创建，无需关闭
        if (holder.cancel(true)) {
            return completedStage(null);
        }

        // 已连接成功：优雅关闭底层 transport
        if (holder.isDone() && !holder.isCancelled()) {
            return holder.thenCompose(Hold::closeGracefully)
                    .exceptionally(ex -> {
                        logger.warn("{} closing holder failed.", this, ex);
                        return null;
                    });
        }

        // 已被其他地方关闭或其他状态
        return completedStage(null);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public Mono<Void> closeGracefully() {

        if (isClosed() || !closeF.complete(null)) {
            return Mono.empty();
        }

        // 关闭PINGER
        pinger.close();

        // 关闭调度器
        synchronized (this) {
            if (ownsScheduler && null != scheduler) {
                scheduler.shutdownNow();
                scheduler = null;
            }
        }

        return Mono.fromCompletionStage(closingHolder())
                .doFinally(s -> logger.debug("{} closed.", this));

    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

        if (isClosed()) {
            return Mono.error(new IllegalStateException("Already closed!"));
        }

        final var holder = getHolder();
        final var sendF = holder.thenCompose(hold ->
                hold.transport().sendMessage(message)
                        .toFuture()

                        /*
                         * 发送失败则需要理解发起重连，
                         * 但只有能成功重置连接持有者手中的连接实例的请求，才有资格发起立即重连。
                         */
                        .whenComplete((u, ex) -> {
                            if (null != ex) {
                                if (tryResetHolder(holder)) {
                                    schedulingConnectNow(hold.handler());
                                }
                            }
                        }));

        //noinspection NullableProblems
        return Mono.fromFuture(sendF);
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

        if (!connectF.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Duplicate connected!"));
        }

        schedulingConnectNow(pingEnabled ? pinger.wrapHandler(handler::apply) : handler::apply);
        return Mono.empty();
    }


    /**
     * 立即调度连接任务
     *
     * @param handler 处理器
     */
    private void schedulingConnectNow(Handler handler) {
        schedulingConnect(new Attempt(handler, 0, null));
    }


    /**
     * 调度连接任务
     *
     * @param attempt 重连连接上下文
     */
    private void schedulingConnect(Attempt attempt) {

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
            logger.debug("{} reconnect strategy gave up after {} attempts, closing transport.", this, attempt.count());
            close();
            return;
        }

        // 记录重试间隔
        if (null != attempt.cause()) {
            logger.warn("{} connect failed after {} attempts, retrying in {}ms",
                    this,
                    attempt.count(),
                    connectInterval.toMillis(),
                    attempt.cause()
            );
        }

        // 发起连接任务
        scheduling(() -> CompletableFuture.completedStage(null)

                // 创建新连接实例并尝试连接
                .thenCompose(u -> {
                    final var holder = attempt.handler();
                    final var transport = transportFactory.apply(mapper);
                    final var hold = new Hold(transport, holder);
                    return hold.connect();
                })

                // 测试连接是否能正常工作
                .thenCompose(hold -> pinger.ping(hold).thenApply(u -> hold))

                /*
                 * 处理连接结果
                 * 成功：发起PING
                 * 失败：发起重连
                 */
                .whenComplete((hold, ex) -> {

                    // 连接成功
                    if (null == ex) {

                        //  标记连接实例完成
                        final var holder = getHolder();
                        if (holder.complete(hold)) {
                            logger.debug("{} connected after {} attempts.", this, attempt.count());
                            pinger.schedulingPing(holder);
                        }

                        /*
                         * 标记失败，说明连接已经被其他连接请求标记完成。
                         * 这种情况应该不会发生，这里做一个防呆的检查，防止Hold泄露。
                         */
                        else {
                            logger.debug("{} connecting reset after {} attempts.", this, attempt.count());
                            hold.close();
                        }

                    }

                    // 连接失败
                    else {
                        hold.close();
                        schedulingConnect(attempt.next(ex));
                    }

                }), connectInterval);

    }


    /**
     * 任务调度
     *
     * @param task  任务
     * @param delay 执行延迟时间
     * @return 调度回调
     */
    private Future<?> scheduling(Runnable task, Duration delay) {

        synchronized (this) {

            if (isClosed()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Already closed!"));
            }

            if (ownsScheduler && null == scheduler) {
                scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    final Thread thread = new Thread(r);
                    thread.setName("%s/scheduler".formatted(this));
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


    /**
     * 处理器
     * <p>
     * 无他，这里弄一个类来封装，纯粹是因为原来的{@code Function}代码太长。
     * </p>
     */
    @FunctionalInterface
    private interface Handler extends Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> {
    }


    /**
     * 连接实例
     * <p>
     * 一个连接实例由{@code Transport}和{@code Handler}配对组成，代表一个有效连接。
     * </p>
     *
     * @param transport 传输通道
     * @param handler   处理器
     */
    private record Hold(McpClientTransport transport, Handler handler) {

        /**
         * 优雅关闭连接
         *
         * @return 关闭回调
         */
        public CompletionStage<Void> closeGracefully() {
            return transport.closeGracefully().toFuture();
        }

        public void close() {
            transport.close();
        }

        public CompletableFuture<Hold> connect() {
            return transport.connect(handler).toFuture().thenApply(u -> this);
        }

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

    /**
     * Pinger
     * <p>
     * 负责向服务器发送 Ping 请求并等待 Pong 响应，以确保连接的活跃性。
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
        private static final int MAX_SESSIONS = 1000;

        /**
         * 会话映射表：Ping ID -> Pong 回调 Future
         */
        private final Map<String, CompletableFuture<Void>> sessionMap = new ConcurrentHashMap<>();

        /**
         * 连续心跳失败次数
         */
        private final AtomicInteger consecutivePingFailures = new AtomicInteger(0);

        /*
         * FATHER THIS
         */
        private final Object _this = RecoverableMcpClientTransport.this;

        /**
         * 调度一个 Ping 任务
         *
         * @param holder 连接实例持有者
         * @return 调度结果
         */
        @SuppressWarnings("UnusedReturnValue")
        public Future<?> schedulingPing(CompletableFuture<Hold> holder) {

            if (!pingEnabled) {
                return CompletableFuture.completedFuture(null);
            }

            return scheduling(() -> {
                holder.thenCompose(hold -> ping(hold)
                        .thenAccept(u -> {
                            logger.debug("{} ping success.", _this);
                            consecutivePingFailures.set(0);
                            schedulingPing(holder);
                        })
                        .exceptionally(ex -> {
                            final var failures = consecutivePingFailures.incrementAndGet();
                            if (failures <= maxConsecutivePingFailures) {
                                logger.warn("{} ping failed, not giving up yet! failures={}", _this, failures, ex);
                                schedulingPing(holder);
                            } else {
                                if (tryResetHolder(holder)) {
                                    logger.warn("{} ping failed, giving up! failures={}", _this, failures, ex);
                                    schedulingConnectNow(hold.handler());
                                } else {
                                    logger.warn("{} ping failed, ignored by reset! failures={}", _this, failures, ex);
                                    consecutivePingFailures.set(0);
                                }
                            }
                            return null;
                        }));
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
                logger.warn("{} session map full ({} sessions), rejecting ping.", _this, MAX_SESSIONS);
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

                        // 提前失败回来的
                        if (null != ex && !pongF.isDone()) {
                            pongF.completeExceptionally(ex);
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

                /*
                 * 起手就尝试将PONG回调标记取消，
                 * 如果能取消成功，说明PONG还没到；否则说明PONG已经在超时之前完成。
                 */
                if (pongF.cancel(true)) {
                    logger.debug("{} pong timeout, no response received. id={}", _this, pingId);
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

    public static class Builder implements Buildable<RecoverableMcpClientTransport, Builder> {

        private String name;
        private McpJsonMapper mapper;
        private ReconnectStrategy reconnectStrategy;
        private Function<McpJsonMapper, McpClientTransport> transportFactory;
        private ScheduledExecutorService scheduler;

        private boolean pingEnabled;
        private Duration pingInterval;
        private Duration pingTimeout;
        private Integer maxConsecutivePingFailures;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

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
        public RecoverableMcpClientTransport build() {
            return new RecoverableMcpClientTransport(this);
        }

    }


}
