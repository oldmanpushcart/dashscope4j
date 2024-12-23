package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;

import java.util.concurrent.CompletionStage;

public interface Interceptor {

    CompletionStage<?> intercept(Chain chain);

    interface Chain {

        DashscopeClient client();

        ApiRequest<?> request();

        CompletionStage<?> process(ApiRequest<?> request);

    }

}
