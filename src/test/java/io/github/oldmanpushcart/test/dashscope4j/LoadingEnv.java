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

                    // base file
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/base/file-")
                            .periodByQPS()
                            .strategyByDelay()
                            .maxAcquired(1)
                            .build(),

                    // chat qwen-long
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-long")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(100)
                            .build(),

                    // chat qwen-turbo
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-turbo")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(500)
                            .maxUsage("total_tokens",500000)
                            .build(),

                    // chat qwen-plus
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-plus")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(200)
                            .maxUsage("total_tokens",2000000)
                            .build(),

                    // chat qwen-max
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-max")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(60)
                            .maxUsage("total_tokens",100000)
                            .build(),

                    // chat qwen-audio-turbo
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-audio-turbo")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(120)
                            .maxUsage("total_tokens",100000)
                            .build(),

                    // chat qwen-audio-chat
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-audio-chat")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(120)
                            .maxUsage("total_tokens",100000)
                            .build()
            )
            .build();


    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .interceptors(invokeCountInterceptor, rateLimitInterceptor)
            .build();

}
