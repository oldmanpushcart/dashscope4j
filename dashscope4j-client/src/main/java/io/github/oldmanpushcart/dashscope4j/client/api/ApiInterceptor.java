package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

import java.util.concurrent.CompletionStage;

public interface ApiInterceptor {

    CompletionStage<?> intercept(Chain chain);

    interface Chain {

        DashscopeClient client();

        ApiRequest<?> request();

        CompletionStage<?> proceed();

        CompletionStage<?> proceed(ApiRequest<?> request);

    }

}
