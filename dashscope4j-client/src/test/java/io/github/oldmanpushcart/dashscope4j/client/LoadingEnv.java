package io.github.oldmanpushcart.dashscope4j.client;

import java.net.http.HttpClient;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    DashscopeClient client = DashscopeClient.newBuilder()
            .ak(AK)
            .http(HttpClient.newBuilder()
                    .build())
            .build();

}
