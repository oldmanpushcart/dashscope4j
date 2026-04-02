package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

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
 * Recoverable McpClientTransport wrapper with auto-reconnect capability.
 */
public class RecoverableMcpClientTransport implements McpClientTransport {

    private static final String NAME = "mcp-client-transport/recoverable";
    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_PING_TIMEOUT = Duration.ofSeconds(60); // 2x ping interval
    private static final int DEFAULT_MAX_CONSECUTIVE_SEND_FAILURES = 5;
    private static final int DEFAULT_MAX_CONSECUTIVE_PING_FAILURES = 5;
    private static final int MAX_RETRY_ATTEMPT = 63; // Prevent overflow in exponential backoff
    private static final ReconnectStrategy DEFAULT_RECONNECT_STRATEGY = ReconnectStrategies
            .exponentialBackoff(
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(1),
                    0.3
            );

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final McpJsonMapper mapper;
    private final McpClientTransportFactory transportFactory;
    private final ReconnectStrategy reconnectStrategy;
    private final int maxConsecutiveSendFailures;

    private final boolean ownsScheduler;
    private final ScheduledExecutorService scheduler;

    private final boolean pingEnabled;
    private final Duration pingInterval;
    private final Duration pingTimeout; // Timeout for each ping request
    private final int maxConsecutivePingFailures;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Hold>> holderRef = new AtomicReference<>(new CompletableFuture<>());
    private final Pinger pinger = new Pinger();
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
                    ? Mono.fromCompletionStage(cachedClosing)
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

        return Mono.fromCompletionStage(closingF);
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
     * Get the holder
     */
    private CompletableFuture<Hold> getHolder() {
        return holderRef.get();
    }

    /**
     * Try to reset the holder instance
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
     * Close the old holder gracefully
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
     * Scheduling task with delay
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
     * Reconnect context
     */
    private record ReconnectContext(Handler handler, int attemptCount, Throwable failureCause) {
        
        ReconnectContext nextAttempt(Throwable cause) {
            return new ReconnectContext(handler, attemptCount + 1, cause);
        }
    }

    /**
     * Scheduling connect immediately
     */
    private Future<?> schedulingConnectNow(Handler handler) {
        return schedulingConnect(new ReconnectContext(handler, 0, null));
    }


    /**
     * Scheduling connect with retry
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
     * Hold transport and handler
     */
    private static class Hold {

        private final McpClientTransport transport;
        private final Handler handler;

        private Hold(McpClientTransport transport, Handler handler) {
            this.transport = transport;
            this.handler = handler;
        }

        /**
         * Close the transport gracefully
         *
         * @return close future
         */
        public Mono<Void> closeGracefully() {
            return transport.closeGracefully();
        }

    }

    /**
     * NetworkHealth tracks network connection quality and triggers reconnection when needed.
     * <p>
     * This class centralizes all failure counting and reconnection logic to avoid
     * duplicate state management in Hold instances.
     * </p>
     */
    private class NetworkHealth {

        private final AtomicInteger consecutiveSendFailures = new AtomicInteger(0);
        private final AtomicInteger consecutivePingFailures = new AtomicInteger(0);
        private final AtomicBoolean reconnecting = new AtomicBoolean(false);

        /**
         * Notify that a send operation succeeded.
         * Resets the consecutive send failure counter.
         */
        public void notifySendSuccess() {
            consecutiveSendFailures.set(0);
        }

        /**
         * Notify that a send operation failed.
         * Triggers reconnection if failure count exceeds threshold.
         *
         * @param trigger reconnection trigger callback
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
         * Notify that a ping succeeded.
         * Resets the consecutive ping failure counter.
         */
        public void notifyPingSuccess() {
            consecutivePingFailures.set(0);
            reconnecting.set(false); // Reset reconnect flag on successful ping
        }

        /**
         * Notify that a ping failed.
         * Triggers reconnection if failure count exceeds threshold.
         *
         * @param trigger reconnection trigger callback
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
         * Reset all counters. Called after successful reconnection.
         */
        public void reset() {
            consecutiveSendFailures.set(0);
            consecutivePingFailures.set(0);
            reconnecting.set(false);
        }

    }


    /**
     * Factory interface for creating McpClientTransport instances.
     */
    public interface McpClientTransportFactory {

        /**
         * Create a new McpClientTransport instance.
         *
         * @param mapper object mapper
         * @return McpClientTransport instance
         */
        McpClientTransport create(McpJsonMapper mapper);

    }

    /**
     * Reconnect strategy interface.
     */
    public interface ReconnectStrategy {

        /**
         * Calculate the retry delay based on the attempt count and failure cause.
         *
         * @param attemptCount attempt count
         * @param failureCause failure cause
         * @return retry delay
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
     * Pinger class for monitoring the connection health.
     * <p>
     * Simplified design: uses direct callbacks instead of handler wrapping.
     * Includes session capacity limit and expiration cleanup.
     * </p>
     */
    private class Pinger implements AutoCloseable {

        private static final String PREFIX = "HB-PING";
        // Limit session map size to prevent memory exhaustion
        private static final int MAX_SESSIONS = 1000;
        private final Map<String, CompletableFuture<Void>> sessionMap = new ConcurrentHashMap<>();

        /**
         * Ping the server and schedule a pong response.
         *
         * @param hold hold instance
         * @return CompletionStage that completes when pong is received
         * @throws IllegalStateException if session map is full
         */
        public CompletionStage<?> ping(Hold hold) {

            /*
             * Ping request identity
             *
             * FORMAT: PREFIX-UUID
             * We need a special prefix to distinguish between ping and normal requests.
             */
            final var pingId = "%s-%s".formatted(PREFIX, UUID.randomUUID().toString());

            // pong callback future
            final var pongF = new CompletableFuture<Void>();

            /*
             * Check session map capacity to prevent memory exhaustion
             */
            if (sessionMap.size() >= MAX_SESSIONS) {
                logger.warn("{} session map full ({} sessions), rejecting ping.", this, MAX_SESSIONS);
                pongF.completeExceptionally(new IllegalStateException("Session map capacity exceeded"));
                return pongF;
            }

            /*
             * Ping timeout task
             *
             * If the pong is not received within the specified timeout,
             * the ping session will be removed and the pong callback future will be cancelled.
             */
            final var timeoutF = schedulingPingTimeout(pingId, pongF);

            /*
             * Register to session
             *
             * If the pingId matches, the pong callback future will be completed.
             */
            sessionMap.put(pingId, pongF);

            /*
             * Send ping request
             *
             * If send ping failed, the pong callback future will be removed from sessionMap,
             * and the ping timeout task will be cancelled.
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
         * Schedule a ping timeout.
         *
         * @param pingId pingId
         * @param pongF  pong callback future
         * @return ping timeout future
         */
        private Future<?> schedulingPingTimeout(String pingId, CompletableFuture<Void> pongF) {
            return scheduling(() -> {
                if (pongF.cancel(true)) {
                    logger.warn("{}/heartbeat/{} timeout!", RecoverableMcpClientTransport.this, pingId);
                }
            }, pingTimeout); // Use dedicated timeout, not interval
        }

        /**
         * Check if the specified requestId is a pingId.
         *
         * @param requestId requestId
         * @return true if the requestId is a pingId, false otherwise
         */
        private boolean isPingId(String requestId) {
            return requestId.startsWith(PREFIX);
        }

        /**
         * Wrap the handler to intercept ping responses.
         * <p>
         * This method is called during transport initialization to set up the message filter.
         * When a ping response is received, it completes the corresponding future and removes
         * it from the session map.
         * </p>
         *
         * @param delegate original handler
         * @return wrapped handler that filters ping responses
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<RecoverableMcpClientTransport, Builder> {

        private McpJsonMapper mapper;
        private McpClientTransportFactory transportFactory;
        private ReconnectStrategy reconnectStrategy;
        private ScheduledExecutorService scheduler;
        private int maxConsecutiveSendFailures = DEFAULT_MAX_CONSECUTIVE_SEND_FAILURES;

        private boolean pingEnabled = true;
        private Duration pingInterval = DEFAULT_PING_INTERVAL;
        private Duration pingTimeout = DEFAULT_PING_TIMEOUT;
        private int maxConsecutivePingFailures = DEFAULT_MAX_CONSECUTIVE_PING_FAILURES;


        public Builder mapper(McpJsonMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder transportFactory(McpClientTransportFactory transportFactory) {
            this.transportFactory = transportFactory;
            return this;
        }

        public Builder reconnectStrategy(ReconnectStrategy reconnectStrategy) {
            this.reconnectStrategy = reconnectStrategy;
            return this;
        }

        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder maxConsecutiveSendFailures(int maxConsecutiveSendFailures) {
            this.maxConsecutiveSendFailures = maxConsecutiveSendFailures;
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

        public Builder pingEnabled(boolean pingEnabled) {
            this.pingEnabled = pingEnabled;
            return this;
        }

        @Override
        public RecoverableMcpClientTransport build() {
            return new RecoverableMcpClientTransport(this);
        }

    }

}
