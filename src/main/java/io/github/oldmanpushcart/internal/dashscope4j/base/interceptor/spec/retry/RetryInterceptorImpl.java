package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.retry;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.retry.RetryInterceptor;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class RetryInterceptorImpl implements RetryInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_NAME);
    private final Matcher matcher;
    private final Integer maxRetries;
    private final Duration retryInterval;

    private RetryInterceptorImpl(Builder builder) {
        this.matcher = builder.matcher;
        this.maxRetries = builder.maxRetries;
        this.retryInterval = builder.retryInterval;
    }

    @Override
    public CompletableFuture<?> handle(InvocationContext context, ApiRequest request, OpHandler opHandler) {
        return opHandler.handle(request)
                .exceptionallyCompose(ex -> retryInEx(0, ex, context, request, opHandler)
                        .thenApply(CommonUtils::cast));
    }


    /**
     * 失败重试
     *
     * @param retries   重试次数
     * @param ex        异常
     * @param context   调用上下文
     * @param request   请求
     * @param opHandler 操作处理器
     * @return 异步结果
     */
    private CompletableFuture<?> retryInEx(int retries, Throwable ex, InvocationContext context, ApiRequest request, OpHandler opHandler) {

        // 不匹配或达到最大重试次数
        if (!matcher.matches(context, request, ex) || (!isNull(maxRetries) && retries >= maxRetries)) {
            return CompletableFuture.failedFuture(ex);
        }

        // 执行器
        final Executor executor;
        if (null != retryInterval) {
            final var retryIntervalMs = retryInterval.toMillis();
            executor = CompletableFuture.delayedExecutor(retryIntervalMs, TimeUnit.MILLISECONDS, context.executor());
            logger.debug("{}/retry/{}, will be retry after {} ms", request.protocol(), retries, retryIntervalMs, ex);
        } else {
            executor = context.executor();
            logger.debug("{}/retry/{}", request.protocol(), retries, ex);
        }

        // 重试
        return CompletableFuture.supplyAsync(() -> opHandler.handle(request), executor)
                .thenCompose(v -> v)
                .exceptionallyCompose(_ex ->
                        retryInEx(retries + 1, _ex, context, request, opHandler)
                                .thenApply(CommonUtils::cast));
    }


    /**
     * 重试拦截器构建器
     */
    public static class Builder implements RetryInterceptor.Builder {

        private Matcher matcher;
        private Integer maxRetries;
        private Duration retryInterval;

        @Override
        public RetryInterceptor.Builder matches(Matcher matcher) {
            this.matcher = requireNonNull(matcher);
            return this;
        }

        @Override
        public RetryInterceptor.Builder maxRetries(int maxRetries) {
            this.maxRetries = CommonUtils.check(maxRetries, v -> v > 0, "maxRetries must be positive!");
            return this;
        }

        @Override
        public RetryInterceptor.Builder retryInterval(Duration retryInterval) {
            this.retryInterval = requireNonNull(retryInterval);
            return this;
        }

        @Override
        public RetryInterceptor build() {
            requireNonNull(matcher, "matcher is required!");
            return new RetryInterceptorImpl(this);
        }

    }

}
