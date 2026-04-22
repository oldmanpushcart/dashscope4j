package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.RetryInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.retry.RetryStrategies;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.List;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    DashscopeClient client = DashscopeClient.newBuilder()
            .ak(AK)
            .http(new OkHttpClient.Builder()
                    .pingInterval(Duration.ofSeconds(60))
                    .connectTimeout(Duration.ofSeconds(3))
                    .readTimeout(Duration.ofSeconds(60))
                    .writeTimeout(Duration.ofSeconds(60))
                    .build())
//            .interceptors(List.of(
//                    RetryInterceptor.newBuilder()
//                            .strategy(RetryStrategies.fixedDelay(Duration.ofSeconds(5)))
//                            .build()
//            ))
            .build();

}
