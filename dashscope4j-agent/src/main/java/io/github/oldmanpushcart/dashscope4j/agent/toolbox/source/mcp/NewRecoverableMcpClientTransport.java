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
import java.util.Objects;
import java.util.concurrent.*;
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

    public NewRecoverableMcpClientTransport(Builder builder) {
        Objects.requireNonNull(builder.reconnectStrategy, "reconnectStrategy must not be null!");
        Objects.requireNonNull(builder.transportFactory, "transportFactory must not be null!");
        this.mapper = requireNonNullElseGet(builder.mapper, () -> new JacksonMcpJsonMapper(JacksonJsonUtils.newMapper()));
        this.reconnectStrategy = builder.reconnectStrategy;
        this.transportFactory = builder.transportFactory;
        this.scheduler = builder.scheduler;
        this.ownsScheduler = null == builder.scheduler;
    }

    private boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public Mono<Void> closeGracefully() {

        if (isClosed() || !closeF.complete(null)) {
            return Mono.empty();
        }

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

        task = schedulingConnectNow(handler::apply);
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

        // 如果充实间隔为null，说明本次放弃重试
        if (null == connectInterval) {
            return CompletableFuture.failedFuture(new IllegalStateException("Give up reconnect by error!", attempt.cause()));
        }

        return scheduling(() -> {
            final var transport = transportFactory.apply(mapper);
            transport.connect(attempt.handler())
                    .doOnSuccess(u -> {
                        holder.set(new Hold(transport, attempt.handler()));
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

        @Override
        public NewRecoverableMcpClientTransport build() {
            return new NewRecoverableMcpClientTransport(this);
        }

    }


}
