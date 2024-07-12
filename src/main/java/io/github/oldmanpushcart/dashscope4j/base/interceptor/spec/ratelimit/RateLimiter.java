package io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit.RateLimiterBuilderImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * 限流
 */
public interface RateLimiter {

    /**
     * @return 限流周期
     */
    Duration period();

    /**
     * 限流
     *
     * @param context 调用上下文
     * @param request 发送请求
     * @param metric  周期度量
     * @return 限流策略
     */
    Strategy limit(InvocationContext context, ApiRequest<?> request, Metric metric);

    /**
     * 限制策略
     */
    enum Strategy {

        /**
         * 通过
         */
        PASS,

        /**
         * 跳过
         */
        SKIP,

        /**
         * 延迟到下个周期
         */
        DELAY,

        /**
         * 阻塞
         */
        BLOCK

    }

    /**
     * 周期常量
     */
    interface Periods {

        /**
         * QPS
         */
        Duration QPS = Duration.ofSeconds(1);

        /**
         * QPM
         */
        Duration QPM = Duration.ofMinutes(1);

    }

    /**
     * 匹配器常量
     */
    interface Matchers {

        /**
         * 协议前缀匹配
         *
         * @param prefix 前缀表达式
         * @return 匹配器
         */
        static BiPredicate<InvocationContext, ApiRequest<?>> matchesByProtocolPrefix(String prefix) {
            return (context, request) -> request.protocol().startsWith(prefix);
        }

        /**
         * 协议正则匹配
         *
         * @param regex 正则表达式
         * @return 匹配器
         */
        static BiPredicate<InvocationContext, ApiRequest<?>> matchesByProtocolRegex(String regex) {
            return (context, request) -> request.protocol().matches(regex);
        }

        /**
         * 请求类型匹配
         *
         * @param clazz 请求类型
         * @return 匹配器
         */
        static BiPredicate<InvocationContext, ApiRequest<?>> matchesByRequestType(Class<? extends ApiRequest<?>> clazz) {
            return (context, request) -> clazz.isInstance(request);
        }

        /**
         * 模型匹配
         *
         * @param model 模型
         * @return 匹配器
         */
        static BiPredicate<InvocationContext, ApiRequest<?>> matchesByModel(Model model) {
            return matchesByModel(model.name());
        }

        /**
         * 模型匹配
         *
         * @param name 模型名称
         * @return 匹配器
         */
        static BiPredicate<InvocationContext, ApiRequest<?>> matchesByModel(String name) {
            return (context, request) -> request instanceof AlgoRequest<?, ?> algoRequest && algoRequest.model().name().equals(name);
        }

    }

    /**
     * 周期度量
     */
    interface Metric {

        /**
         * @return 已发送请求
         */
        int acquired();

        /**
         * @return 已应答成功
         */
        int succeed();

        /**
         * @return 已应答失败
         */
        int failed();

        /**
         * @return 统计周期开始时间戳
         */
        Instant since();

        /**
         * @return 已消耗度量
         */
        Usage usage();

    }

    /**
     * 阻塞异常
     */
    class BlockException extends RuntimeException {

        private final ApiRequest<?> request;

        /**
         * 构建阻塞异常
         *
         * @param request 发送请求
         */
        public BlockException(ApiRequest<?> request) {
            this.request = request;
        }

        /**
         * @return 阻塞发生时的发送请求
         */
        public ApiRequest<?> request() {
            return request;
        }

        @Override
        public String getLocalizedMessage() {
            return "rate-limit blocked: %s".formatted(request.protocol());
        }

    }

    /**
     * @return 限流器构建器
     */
    static Builder newBuilder() {
        return new RateLimiterBuilderImpl();
    }

    /**
     * 构建器
     */
    interface Builder extends Buildable<RateLimiter, Builder> {

        /**
         * 设置限流匹配器
         *
         * @param matcher 匹配器
         * @return this
         */
        Builder matches(BiPredicate<InvocationContext, ApiRequest<?>> matcher);

        /**
         * 设置限流周期
         *
         * @param period 限流周期
         * @return this
         */
        Builder period(Duration period);

        /**
         * 设置限流策略
         *
         * @param strategy 限流策略
         * @return this
         */
        Builder strategy(Strategy strategy);

        /**
         * 设置最大允许请求数
         *
         * @param maxAcquired 最大允许请求数
         * @return this
         */
        Builder maxAcquired(int maxAcquired);

        /**
         * 设置最大允许成功请求数
         *
         * @param maxSucceed 最大允许成功请求数
         * @return this
         */
        Builder maxSucceed(int maxSucceed);

        /**
         * 设置最大允许失败请求数
         *
         * @param maxFailed 最大允许失败请求数
         * @return this
         */
        Builder maxFailed(int maxFailed);

        /**
         * 设置最大允许耗用量
         *
         * @param names   用量名称集合
         * @param maxCost 最大用量
         * @return this
         */
        Builder maxUsage(Set<String> names, int maxCost);

        /**
         * 设置最大允许耗用量
         *
         * @param maxCost 最大用量
         * @return this
         */
        default Builder maxUsage(int maxCost) {
            return maxUsage(Set.of(), maxCost);
        }

    }

}
