package io.github.oldmanpushcart.dashscope4j.agent.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;

import java.util.concurrent.CompletionStage;

public class FailureRetryInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {
        return null;
    }
    
}
