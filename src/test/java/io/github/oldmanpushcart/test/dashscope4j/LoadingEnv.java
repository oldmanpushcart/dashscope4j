package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;
import io.github.oldmanpushcart.test.dashscope4j.base.interceptor.InvokeCountInterceptor;

import java.util.Set;
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
                            .maxAcquired(10)
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
                            .maxUsage(Set.of("total_tokens"), 500000)
                            .build(),

                    // chat qwen-plus
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-plus")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(200)
                            .maxUsage(Set.of("total_tokens"), 2000000)
                            .build(),

                    // chat qwen-max
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-max")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(60)
                            .maxUsage(Set.of("total_tokens"), 100000)
                            .build(),

                    // chat qwen-audio-turbo
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-audio-turbo")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(120)
                            .maxUsage(100000)
                            .build(),

                    // chat qwen-audio-chat
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-audio-chat")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(120)
                            .maxUsage(100000)
                            .build(),

                    // chat qwen-vl-plus 100000
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-vl-plus")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(60)
                            .maxUsage(100000)
                            .build(),

                    // chat qwen-vl-max
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/chat/qwen-vl-max")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(15)
                            .maxUsage(25000)
                            .build(),

                    // embedding text-embedding-v1 QPS
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/embedding/text-embedding-v1")
                            .periodByQPS()
                            .strategyByDelay()
                            .maxAcquired(30)
                            .build(),

                    // embedding text-embedding-v1 QPS
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/embedding/text-embedding-v1")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxUsage(600000)
                            .build(),

                    // embedding text-embedding-v2 QPS
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/embedding/text-embedding-v2")
                            .periodByQPS()
                            .strategyByDelay()
                            .maxAcquired(30)
                            .build(),

                    // embedding text-embedding-v2 QPS
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/embedding/text-embedding-v2")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxUsage(600000)
                            .build(),

                    //
                    RateLimiter.newBuilder()
                            .matchesByProtocolPrefix("/dashscope/mm-embedding/multimodal-embedding-one-peace-v1")
                            .periodByQPM()
                            .strategyByDelay()
                            .maxAcquired(20)
                            .maxUsage(20)
                            .build()

                    )
            .build();


    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .interceptors(invokeCountInterceptor, rateLimitInterceptor)
            .build();

}
