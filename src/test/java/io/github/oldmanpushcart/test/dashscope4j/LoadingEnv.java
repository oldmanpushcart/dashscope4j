package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;
import io.github.oldmanpushcart.test.dashscope4j.base.interceptor.InvokeCountInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    ExecutorService executor = Executors.newFixedThreadPool(10);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    InvokeCountInterceptor invokeCountInterceptor = new InvokeCountInterceptor();
    Interceptor rateLimitInterceptor = RateLimitInterceptor.newBuilder()
            .scheduler(scheduler)
            .limiters(

                    // fileOp 限流
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/base/file-")
                            .periodByQPS()
                            .strategyByDelay()
                            .maxAcquired(1)
                            .build()
            )
            .build();


    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .interceptors(invokeCountInterceptor, rateLimitInterceptor)
            .build();

}
