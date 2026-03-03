package io.github.oldmanpushcart.dashscope4j.client;

import okhttp3.OkHttpClient;

import java.time.Duration;

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
            .build();

}
