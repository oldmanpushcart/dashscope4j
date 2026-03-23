package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

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
 * Recoverable McpClientTransport
 * <p>
 * Recoverable McpClientTransport is a wrapper of McpClientTransport, which can recover from connection failures.
 * It can recover from connection failures by reconnecting to the server.
 * </p>
 */
public class RecoverableMcpClientTransport implements McpClientTransport {

    private static final String NAME = "mcp-client-transport/recoverable";
    private static final Duration DEFAULT_PING_INTERVAL = Duration.ofSeconds(30);
    private static final int DEFAULT_MAX_CONSECUTIVE_SEND_FAILURES = 5;
    private static final int DEFAULT_MAX_CONSECUTIVE_PING_FAILURES = 5;
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
    private final int maxConsecutivePingFailures;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<Hold>> holderRef = new AtomicReference<>(new CompletableFuture<>());
    private final Pinger pinger = new Pinger();

    private RecoverableMcpClientTransport(Builder builder) {

        requireNonNull(builder.transportFactory, "transportFactory must not be null.");

        this.mapper = requireNonNullElseGet(builder.mapper, () -> new JacksonMcpJsonMapper(JacksonJsonUtils.newMapper()));
        this.transportFactory = builder.transportFactory;
        this.reconnectStrategy = requireNonNullElse(builder.reconnectStrategy, DEFAULT_RECONNECT_STRATEGY);
        this.maxConsecutiveSendFailures = builder.maxConsecutiveSendFailures;

        /*
         * Configure pinger
         */
        {
            this.pingEnabled = builder.pingEnabled;
            this.pingInterval = requireNonNullElse(builder.pingInterval, DEFAULT_PING_INTERVAL);
            this.maxConsecutivePingFailures = builder.maxConsecutivePingFailures;
        }

        /*
         * Create scheduler if necessary
         *
         * mark ownsScheduler as true if the scheduler is created by this class,
         * owned scheduler will be shutdown by closeGracefully
         */
        if (Objects.isNull(builder.scheduler)) {
            this.ownsScheduler = true;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r ->
                    new Thread(r) {{
                        setName("%s/scheduler".formatted(NAME));
                        setDaemon(true);
                    }});
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

        // Do nothing if the transport is closed
        if (!closed.compareAndSet(false, true)) {
            return Mono.empty();
        }

        // main closing
        final var closingF = CompletableFuture.completedStage(null)

                // closing pinger
                .thenAccept(unused -> pinger.close())

                // closing holder
                .thenCompose(unused -> closingHolder())
                .exceptionally(ex -> {
                    logger.debug("{} closing holder failed.", this, ex);
                    return null;
                })

                // closing scheduler
                .thenCompose(unused -> closingSchedulerIfNecessary())
                .exceptionally(ex -> {
                    logger.debug("{} closing scheduler failed.", this, ex);
                    return null;
                })

                // finally closed
                .thenAccept(unused -> logger.debug("{} closed.", this));

        return Mono.fromCompletionStage(closingF);
    }


    /*
     * Closing the holder
     *
     * 1. if the holder is running, then cancel it
     * 2. if the holder is done, then close it
     */
    private CompletionStage<Void> closingHolder() {
        final var holder = getHolder();
        if (!holder.cancel(true)
                && holder.isDone()) {
            return holder
                    .thenCompose(hold -> hold.closeGracefully().toFuture());
        } else {
            return completedStage(null);
        }
    }

    /*
     * Closing the scheduler
     *
     * 1. if the scheduler is internal, then shutdown it
     * 2. if the scheduler is external, then do nothing
     */
    private CompletionStage<Void> closingSchedulerIfNecessary() {
        if (!ownsScheduler) {
            return completedStage(null);
        }
        final var closingF = new CompletableFuture<Void>();
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                closingF.completeExceptionally(new IllegalStateException("Failed to close scheduler!"));
            } else {
                closingF.complete(null);
            }
        } catch (InterruptedException ex) {
            closingF.completeExceptionally(ex);
        }
        return closingF;
    }

    /**
     * Sends a message using the current transport.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Checks if the transport is closed or not ready — skips sending if so.</li>
     *   <li>Ensures the current holder is done before proceeding.</li>
     *   <li>Sends the message asynchronously through the transport.</li>
     *   <li>On success: resets the consecutive send failure counter.</li>
     *   <li>On failure: increments the failure counter and attempts to reset the holder
     *       if the threshold is exceeded, possibly triggering a reconnect.</li>
     * </ul>
     * </p>
     *
     * @param message the JSON-RPC message to be sent
     * @return a Mono that completes when the send operation is done (either successfully or with error)
     */
    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

        // Skip sending if the transport has already been closed
        if (isClosed()) {
            return Mono.empty();
        }

        // Get the current holder
        final var holder = getHolder();

        /*
         * Send the message and handle result:
         *
         * 1. If successful → reset consecutive failure counter
         * 2. If failed → increment failure count and try to reset the holder if threshold is reached
         *    (may trigger reconnection logic)
         */
        final var sendF = holder.thenCompose(hold ->

                // Perform the actual message sending
                hold.transport.sendMessage(message)
                        .toFuture()

                        // On success: reset consecutive failure count
                        .thenAccept(unused -> hold.notifySendSuccess())

                        // On failure: handle send error
                        .exceptionallyCompose(ex -> {
                            hold.notifySendFailure(() -> {
                                if (tryResetHolder(holder)) {
                                    schedulingConnectNow(hold.handler);
                                }
                            });
                            return CompletableFuture.failedStage(ex);
                        }));

        return Mono.fromFuture(sendF);
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return mapper.convertValue(data, typeRef);
    }

    /**
     * Check if the transport is closed
     *
     * @return true if the transport is closed, false otherwise
     */
    private boolean isClosed() {
        return closed.get();
    }


    /**
     * Get the holder
     * <p>
     * The holder must be reacquired every time it is used to prevent using a potentially reset instance.
     * </p>
     *
     * @return the holder
     */
    private CompletableFuture<Hold> getHolder() {
        return holderRef.get();
    }

    /**
     * Try to reset the holder instance.
     * <p>
     * If the reset is successful, the old holder instance will be closed.<br/>
     * If the reset fails, it indicates that another thread has already reset it concurrently.
     * </p>
     *
     * @param holder holder instance
     * @return true if the reset is successful, false otherwise
     */
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
     * Scheduling task with delay
     * <p>
     * if delay is Zero, then will be scheduling immediately
     * </p>
     *
     * @param task  task
     * @param delay delay
     * @return scheduling future
     */
    private Future<?> scheduling(Runnable task, Duration delay) {
        return Objects.isNull(delay) || delay.isZero()
                ? scheduler.submit(task)
                : scheduler.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Scheduling connect immediately
     *
     * @param handler transport handler
     */
    private Future<?> schedulingConnectNow(Handler handler) {
        return schedulingConnect(handler, 0, null);
    }


    /**
     * Scheduling connect with retry
     *
     * @param handler      transport handler
     * @param attemptCount attempt count
     * @param failureCause failure cause
     */
    private Future<?> schedulingConnect(Handler handler, int attemptCount, Throwable failureCause) {

        /*
         * Compute the connection interval based on retry attempt count.
         *
         * If attemptCount is 0, it indicates the first connection or an immediate reconnect,
         * in which case the interval is set to zero.
         */
        final var connectInterval = attemptCount == 0
                ? Duration.ZERO
                : reconnectStrategy.retryDelay(attemptCount, failureCause);

        /*
         * schedule connect
         *
         * if successful, then schedule heartbeat
         * else schedule reconnect
         */
        return scheduling(() -> completedStage(null)
                .thenApply(unused -> transportFactory.create(mapper))

                // Connect transport
                .thenCompose(transport -> transport.connect(handler)
                        .toFuture()
                        .thenApply(unused -> transport))

                /*
                 * Connect transport success
                 *
                 * complete the holder instance and schedule heartbeat,
                 * if complete failed, close the transport.
                 */
                .thenAccept(transport -> {
                    if (getHolder().complete(new Hold(transport, handler))) {
                        schedulingPing();
                    } else {
                        transport.close();
                    }
                })

                // Connect transport failed, schedule reconnect with connect interval
                .exceptionally(ex -> {
                    schedulingConnect(handler, attemptCount + 1, ex);
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
                        hold.notifyPingSuccess();
                        schedulingPing();
                    })

                    /*
                     * Heartbeat failed
                     *
                     * 1. increment consecutive failure counter
                     * 2. if consecutive failure count exceeds the threshold, reset the holder and schedule reconnect
                     */
                    .exceptionallyCompose(ex -> {
                        hold.notifyPingFailure(() -> {
                            if (tryResetHolder(holder)) {
                                schedulingConnectNow(hold.handler);
                            }
                        });
                        return null;
                    }));

        }, pingInterval);
    }


    /**
     * Hold transport instance and transport handler
     */
    private class Hold {

        private final McpClientTransport transport;
        private final Handler handler;

        /**
         * Consecutive send failure count
         */
        private final AtomicInteger consecutiveSendFailures = new AtomicInteger(0);

        /**
         * Consecutive ping failure count
         */
        private final AtomicInteger consecutivePingFailures = new AtomicInteger(0);

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

        public void notifyPingSuccess() {
            consecutivePingFailures.set(0);
        }

        public void notifyPingFailure(Runnable trigger) {
            final int failures = consecutivePingFailures.incrementAndGet();
            final boolean shouldTrigger = maxConsecutivePingFailures <= 0 || failures > maxConsecutivePingFailures;
            if (shouldTrigger) {
                trigger.run();
            }
        }

        public void notifySendSuccess() {
            consecutiveSendFailures.set(0);
        }

        public void notifySendFailure(Runnable trigger) {
            final int failures = consecutiveSendFailures.incrementAndGet();
            final boolean shouldTrigger = maxConsecutiveSendFailures <= 0 || failures > maxConsecutiveSendFailures;
            if (shouldTrigger) {
                trigger.run();
            }
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


    /**
     * Handler interface for handling messages.
     */
    private interface Handler extends Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> {

    }

    /**
     * Pinger class for monitoring the connection health.
     */
    private class Pinger implements AutoCloseable {

        private static final String PREFIX = "HB-PING";
        private final Map<String, CompletableFuture<Void>> sessionMap = new ConcurrentHashMap<>();

        /**
         * Ping the server and schedule a pong response.
         *
         * @param hold hold instance
         * @return CompletionStage
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
                    logger.debug("{}/heartbeat/{} timeout!", RecoverableMcpClientTransport.this, pingId);
                }
            }, pingInterval);
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
         *
         * @param delegate handler
         * @return wrapped handler
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
