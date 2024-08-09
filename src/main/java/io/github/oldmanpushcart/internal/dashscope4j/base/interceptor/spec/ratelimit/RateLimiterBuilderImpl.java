package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 限流器构建器实现
 */
public class RateLimiterBuilderImpl implements RateLimiter.Builder {

    private RateLimiter.Matcher matcher;
    private Duration period;
    private RateLimiter.Strategy strategy;
    private Integer maxAcquired, maxSucceed, maxFailed, maxUsageCost;
    private final Set<String> maxUsageNameSet = new HashSet<>();

    @Override
    public RateLimiter.Builder matches(RateLimiter.Matcher matcher) {
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
    public RateLimiter.Builder maxUsage(Set<String> names, int maxCost) {
        this.maxUsageNameSet.addAll(names);
        this.maxUsageCost = maxCost;
        return this;
    }

    @Override
    public RateLimiter.Builder strategy(RateLimiter.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    @Override
    public RateLimiter build() {
        Objects.requireNonNull(period);
        Objects.requireNonNull(matcher);
        Objects.requireNonNull(strategy);
        return new RateLimiter() {

            @Override
            public Duration period() {
                return period;
            }

            @Override
            public Strategy limit(InvocationContext context, ApiRequest request, Metric metric) {

                // 不匹配流控规则，跳过流控限制
                if (!matcher.matches(context, request)) {
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
                if(maxUsageCost != null) {
                    final var usageCost = maxUsageNameSet.isEmpty()
                            ? metric.usage().total()
                            : metric.usage().total(e -> maxUsageNameSet.contains(e.name()));
                    return usageCost >= maxUsageCost;
                }

                // 未触发限流
                return false;

            }

        };
    }


}
