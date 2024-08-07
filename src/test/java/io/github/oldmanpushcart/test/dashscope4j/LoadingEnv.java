package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimitInterceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.ratelimit.RateLimiter;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.spec.retry.RetryInterceptor;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.base.interceptor.InvokeCountInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    ExecutorService executor = Executors.newFixedThreadPool(100);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    InvokeCountInterceptor invokeCountInterceptor = new InvokeCountInterceptor();


    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .appendInterceptors(List.of(
                    invokeCountInterceptor,
                    RetryInterceptor.newBuilder()
                            .matches(
                                    RetryInterceptor.Matcher.byProtocol(protocol -> protocol.startsWith("dashscope://base/files/"))
                                            .andThen(RetryInterceptor.Matcher.byApiException(apiEx ->
                                                    apiEx.status() == 429
                                                       && Objects.equals("rate_limit_error", apiEx.ret().code())))
                            )
                            .maxRetries(3)
                            .retryInterval(Duration.ofSeconds(1))
                            .build(),

                    RateLimitInterceptor.newBuilder()
                            .scheduler(scheduler)
                            .limiters(List.of(
                                    RateLimiter.newBuilder()
                                            .matches(RateLimiter.Matcher.byModel(ChatModel.QWEN_PLUS))
                                            .period(RateLimiter.Periods.QPS)
                                            .strategy(RateLimiter.Strategy.DELAY)
                                            .maxAcquired(20)
                                            .build()
                            ))
                            .build()
            ))
            .connectTimeout(Duration.ofMinutes(3))
            .timeout(Duration.ofMinutes(3))
            .build();

}
