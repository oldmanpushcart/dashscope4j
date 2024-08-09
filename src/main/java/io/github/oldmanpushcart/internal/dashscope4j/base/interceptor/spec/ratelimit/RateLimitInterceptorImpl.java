package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;
import io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

/**
 * 限流拦截器
 */
public class RateLimitInterceptorImpl implements RateLimitInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(Constants.LOGGER_NAME);
    private static final String KEY_TOKEN = "RateLimitInterceptor$token";
    private final ScheduledExecutorService scheduler;
    private final RateLimitExecutor executor;

    private RateLimitInterceptorImpl(Builder builder) {
        this.scheduler = builder.scheduler;
        this.executor = new GroupRateLimitExecutor(builder.limiters.stream().map(CasRateLimitExecutor::new).toList());
    }

    @Override
    public CompletableFuture<ApiRequest> preHandle(InvocationContext context, ApiRequest request) {

        final var token = executor.tryAcquire(context, request);
        final var strategy = token.strategy();

        switch (strategy) {

            // 通过
            case PASS -> {
                context.attachmentMap().put(KEY_TOKEN, token);
                return CompletableFuture.supplyAsync(() -> request, context.executor());
            }

            // 跳过
            case SKIP -> {
                return CompletableFuture.completedFuture(request);
            }

            // 阻塞
            case BLOCK -> {
                final var protocol = request.protocol();
                logger.warn("{}/rate-limit/blocked!", protocol);
                return CompletableFuture.failedFuture(new RateLimiter.BlockException(request));
            }

            // 延迟
            case DELAY -> {
                final var protocol = request.protocol();
                final var delayMs = token.delay().toMillis();
                logger.warn("{}/rate-limit/delay={}ms!", protocol, delayMs);
                final var future = new CompletableFuture<ApiRequest>();
                scheduler.schedule(
                        () -> {
                            preHandle(context, request).thenAccept(future::complete);
                        },
                        delayMs,
                        TimeUnit.MILLISECONDS
                );
                return future.thenApply(CommonUtils::cast);
            }

            // 不支持的状态
            default -> throw new UnsupportedOperationException("Unsupported strategy: " + strategy);
        }

    }

    @Override
    public CompletableFuture<ApiResponse<?>> postHandle(InvocationContext context, ApiResponse<?> response, Throwable ex) {

        // 拿到请求时候放入的token，注意，只有PASS的策略才会有值
        final var token = (RateLimitExecutor.Token) context.attachmentMap().remove(KEY_TOKEN);

        // 响应异常
        if (null != ex) {
            if (null != token) {
                token.failure();
            }
            return CompletableFuture.failedFuture(ex);
        }

        // 响应成功
        else {
            if (null != token && response.isLast()) {
                token.success(response.usage());
            }
            return CompletableFuture.completedFuture(response);
        }

    }

    /**
     * 构建器
     */
    public static class Builder implements RateLimitInterceptor.Builder {

        private ScheduledExecutorService scheduler;
        private List<RateLimiter> limiters;

        @Override
        public RateLimitInterceptor.Builder limiters(List<RateLimiter> limiters) {
            this.limiters = requireNonNull(limiters);
            return this;
        }

        @Override
        public RateLimitInterceptor.Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = requireNonNull(scheduler);
            return this;
        }

        @Override
        public RateLimitInterceptor build() {
            check(limiters, CollectionUtils::isNotEmptyCollection, "limiters is empty!");
            requireNonNull(scheduler, "scheduler is required!");
            return new RateLimitInterceptorImpl(this);
        }

    }

}
