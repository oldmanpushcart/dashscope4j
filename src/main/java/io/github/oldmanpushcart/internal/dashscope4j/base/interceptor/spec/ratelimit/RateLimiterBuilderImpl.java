package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * 限流器构建器实现
 */
public class RateLimiterBuilderImpl implements RateLimiter.Builder {

    private BiPredicate<InvocationContext, ApiRequest<?>> matcher;
    private Duration period;
    private RateLimiter.Strategy strategy;
    private Integer maxAcquired, maxSucceed, maxFailed;
    private final Map<String, Integer> maxUsageItemMap = new HashMap<>();

    @Override
    public RateLimiter.Builder matches(BiPredicate<InvocationContext, ApiRequest<?>> matcher) {
        this.matcher = matcher;
        return this;
    }

    @Override
    public RateLimiterBuilderImpl period(Duration period) {
        this.period = period;
        return this;
    }

    @Override
    public RateLimiterBuilderImpl maxAcquired(int maxAcquired) {
        this.maxAcquired = maxAcquired;
        return this;
    }

    @Override
    public RateLimiterBuilderImpl maxSucceed(int maxSucceed) {
        this.maxSucceed = maxSucceed;
        return this;
    }

    @Override
    public RateLimiterBuilderImpl maxFailed(int maxFailed) {
        this.maxFailed = maxFailed;
        return this;
    }

    @Override
    public RateLimiter.Builder maxUsage(String name, int maxCost) {
        this.maxUsageItemMap.put(name, maxCost);
        return this;
    }

    @Override
    public RateLimiter.Builder strategy(RateLimiter.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    private Usage newMaxUsage() {
        return new Usage(
                maxUsageItemMap.entrySet().stream()
                        .map(e -> new Usage.Item(e.getKey(), e.getValue()))
                        .toList()
        );
    }

    @Override
    public RateLimiter build() {
        Objects.requireNonNull(period);
        Objects.requireNonNull(matcher);
        Objects.requireNonNull(strategy);
        final var maxUsage = newMaxUsage();
        return new RateLimiter() {

            @Override
            public Duration period() {
                return period;
            }

            @Override
            public Strategy limit(InvocationContext context, ApiRequest<?> request, Metric metric) {

                // 不匹配流控规则，跳过流控限制
                if (!matcher.test(context, request)) {
                    return Strategy.SKIP;
                }

                // 匹配流控规则，判断是否需要限制
                return isLimited(metric) ? strategy : Strategy.PASS;

            }

            private boolean isLimited(Metric metric) {

                // 限制最大请求量
                if (maxAcquired != null && metric.acquired() >= maxAcquired) {
                    return true;
                }

                // 限制成功请求量
                if (maxSucceed != null && metric.succeed() >= maxSucceed) {
                    return true;
                }

                // 限制失败请求量
                if (maxFailed != null && metric.failed() >= maxFailed) {
                    return true;
                }

                // 限制最大使用量
                for (final var item : metric.usage().items()) {
                    if (item.cost() >= maxUsage.total(e -> e.name().equals(item.name()))) {
                        return true;
                    }
                }

                // 未触发限流
                return false;

            }

        };
    }


}
