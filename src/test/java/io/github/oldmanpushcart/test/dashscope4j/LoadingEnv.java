package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.test.dashscope4j.base.interceptor.InvokeCountInterceptor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface LoadingEnv {

    String AK = System.getenv("DASHSCOPE_AK");

    ExecutorService executor = Executors.newFixedThreadPool(10);

    InvokeCountInterceptor invokeCountInterceptor = new InvokeCountInterceptor();

    DashScopeClient client = DashScopeClient.newBuilder()
            .ak(AK)
            .executor(executor)
            .requestInterceptors(invokeCountInterceptor)
            .responseInterceptors(invokeCountInterceptor)
            .build();

}
