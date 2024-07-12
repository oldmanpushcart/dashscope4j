package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.base.interceptor.InvokeCountInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    ExecutorService executor = Executors.newFixedThreadPool(100);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    InvokeCountInterceptor invokeCountInterceptor = new InvokeCountInterceptor();
    Interceptor rateLimitInterceptor = RateLimitInterceptor.newBuilder()
            .scheduler(scheduler)
            .limiters(List.of(
                    RateLimiter.newBuilder()
                            .matches(RateLimiter.Matchers.matchesByModel(ChatModel.QWEN_PLUS))
                            .period(RateLimiter.Periods.QPS)
                            .strategy(RateLimiter.Strategy.DELAY)
                            .maxAcquired(20)
                            .maxUsage(20)
                            .build()
            ))
            .build();


    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .interceptors(List.of(invokeCountInterceptor, rateLimitInterceptor))
            .connectTimeout(Duration.ofSeconds(60))
            .timeout(Duration.ofSeconds(60))
            .build();

}
