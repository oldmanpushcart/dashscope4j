package io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.spec.ratelimit;

import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 限流执行器组
 */
record GroupRateLimitExecutor(List<? extends RateLimitExecutor> executors) implements RateLimitExecutor {

    @Override
    public Token tryAcquire(InvocationContext context, ApiRequest<?> request) {
        final var tokens = new ArrayList<Token>();
        for (final var executor : executors) {
            final var token = executor.tryAcquire(context, request);
            final var strategy = token.strategy();

            // 申请到则添加到令牌集
            if (strategy == RateLimiter.Strategy.PASS) {
                tokens.add(token);
            }

            // 不是SKIP状态则为被限流或非法，取消之前已经申请的令牌
            else if (strategy != RateLimiter.Strategy.SKIP) {
                tokens.forEach(Token::cancel);
                return token;
            }

        }
        return new TokenImpl(RateLimiter.Strategy.PASS, Duration.ZERO, tokens);
    }

    private record TokenImpl(RateLimiter.Strategy strategy, Duration delay, List<Token> tokens)
            implements Token {

        @Override
        public void success(Usage usage) {
            tokens.forEach(token -> token.success(usage));
        }

        @Override
        public void failure() {
            tokens.forEach(Token::failure);
        }

        @Override
        public void cancel() {
            tokens.forEach(Token::cancel);
        }

    }

}
