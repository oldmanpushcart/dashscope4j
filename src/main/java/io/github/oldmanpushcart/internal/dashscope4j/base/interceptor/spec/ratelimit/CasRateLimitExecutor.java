package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CAS实现的限流执行器
 */
class CasRateLimitExecutor implements RateLimitExecutor {

    private final RateLimiter limiter;
    private final AtomicReference<MetricImpl> metricRef;

    public CasRateLimitExecutor(RateLimiter limiter) {
        this.limiter = Objects.requireNonNull(limiter);
        this.metricRef = new AtomicReference<>(MetricImpl.empty());
    }

    @Override
    public Token tryAcquire(InvocationContext context, ApiRequest<?> request) {
        while (true) {
            final var instant = Instant.now();
            final var metric = updateMetric(instant);
            final var strategy = limiter.limit(context, request, metric);

            // 限流策略：通过；尝试进行申请令牌
            if (strategy == RateLimiter.Strategy.PASS) {
                final var update = new MetricImpl(
                        metric.since(),
                        metric.acquired() + 1,
                        metric.succeed(),
                        metric.failed(),
                        metric.usage()
                );
                if (metricRef.compareAndSet(metric, update)) {
                    return new TokenImpl(strategy, Duration.ZERO);
                }
            }

            // 限流策略：阻塞；阻塞不需要申请令牌
            else if (strategy == RateLimiter.Strategy.BLOCK) {
                return new TokenImpl(strategy, Duration.ZERO);
            }

            // 限流策略：延迟；延迟不需要申请令牌
            else {
                final var diff = Duration.between(metric.since(), instant);
                final var delay = Duration.ofMillis(limiter.period().toMillis() - diff.toMillis());
                return new TokenImpl(strategy, delay);
            }

        }
    }

    private MetricImpl updateMetric(Instant instant) {
        while (true) {
            final var metric = metricRef.get();

            assert metric != null;
            assert metric.acquired() >= 0;
            assert metric.succeed() >= 0;
            assert metric.failed() >= 0;
            assert metric.acquired() >= metric.failed() + metric.succeed();

            if (instant.isAfter(metric.since().plusMillis(limiter.period().toMillis()))) {
                final var update = MetricImpl.next(instant, metric);
                if (metricRef.compareAndSet(metric, update)) {
                    return update;
                }
            } else {
                return metric;
            }
        }
    }

    /**
     * 流控周期度量
     *
     * @param since    开始时间
     * @param acquired 已申请
     * @param succeed  已成功
     * @param failed   已失败
     * @param usage    使用度量
     */
    private record MetricImpl(
            Instant since,
            int acquired,
            int succeed,
            int failed,
            Usage usage
    ) implements RateLimiter.Metric {

        public static MetricImpl next(Instant since, MetricImpl before) {
            final var alive = before.acquired - before.succeed - before.failed;
            return new MetricImpl(since, alive, 0, 0, Usage.empty());
        }

        public static MetricImpl empty(Instant since) {
            return new MetricImpl(since, 0, 0, 0, Usage.empty());
        }

        public static MetricImpl empty() {
            return empty(Instant.now());
        }

    }

    /**
     * 令牌
     */
    private class TokenImpl implements Token {

        private final AtomicBoolean isCompletedRef;
        private final RateLimiter.Strategy strategy;
        private final Duration delay;

        private TokenImpl(RateLimiter.Strategy strategy, Duration delay) {
            this.isCompletedRef = new AtomicBoolean(strategy != RateLimiter.Strategy.PASS);
            this.strategy = strategy;
            this.delay = delay;
        }

        @Override
        public RateLimiter.Strategy strategy() {
            return strategy;
        }

        @Override
        public Duration delay() {
            return delay;
        }

        private void checkAcquired() {
            if (!isAcquired()) {
                throw new IllegalStateException("not acquired!");
            }
        }

        private void completed() {
            if (!isCompletedRef.compareAndSet(false, true)) {
                throw new IllegalStateException("already completed!");
            }
        }

        private static Usage mergeUsages(Usage... usages) {
            final var items = Stream.of(usages)
                    .flatMap(usage -> usage.items().stream())
                    .collect(Collectors.toMap(Usage.Item::name, Usage.Item::cost, Integer::sum))
                    .entrySet().stream()
                    .map(e -> new Usage.Item(e.getKey(), e.getValue()))
                    .toList();
            return new Usage(items);
        }

        @Override
        public void success(Usage usage) {
            checkAcquired();
            completed();
            while (true) {
                final var instant = Instant.now();
                final var metric = updateMetric(instant);
                final var update = new MetricImpl(
                        metric.since(),
                        metric.acquired(),
                        metric.succeed() + 1,
                        metric.failed(),
                        mergeUsages(metric.usage(), usage)
                );
                if (metricRef.compareAndSet(metric, update)) {
                    break;
                }
            }
        }

        @Override
        public void failure() {
            checkAcquired();
            completed();
            while (true) {
                final var instant = Instant.now();
                final var metric = updateMetric(instant);
                final var update = new MetricImpl(
                        metric.since(),
                        metric.acquired(),
                        metric.succeed(),
                        metric.failed() + 1,
                        metric.usage()
                );
                if (metricRef.compareAndSet(metric, update)) {
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            checkAcquired();
            completed();
            while (true) {
                final var instant = Instant.now();
                final var metric = updateMetric(instant);
                final var update = new MetricImpl(
                        metric.since(),
                        metric.acquired() - 1,
                        metric.succeed(),
                        metric.failed(),
                        metric.usage()
                );
                if (metricRef.compareAndSet(metric, update)) {
                    break;
                }
            }
        }

    }

}
