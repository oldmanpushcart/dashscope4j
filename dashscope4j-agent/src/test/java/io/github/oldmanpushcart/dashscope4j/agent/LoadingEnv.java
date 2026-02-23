package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

import java.net.http.HttpClient;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    DashscopeClient client = DashscopeClient.newBuilder()
            .ak(AK)
            .http(HttpClient.newBuilder()
                    .build())
            .build();

}
