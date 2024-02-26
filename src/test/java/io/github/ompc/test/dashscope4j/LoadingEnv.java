package io.github.ompc.test.dashscope4j;

import io.github.ompc.dashscope4j.DashScopeClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface LoadingEnv {

    String SK = System.getenv("DASHSCOPE_SK");

    ExecutorService executor = Executors.newFixedThreadPool(10);

    DashScopeClient client = DashScopeClient.newBuilder()
            .sk(SK)
            .executor(executor)
            .build();

}
