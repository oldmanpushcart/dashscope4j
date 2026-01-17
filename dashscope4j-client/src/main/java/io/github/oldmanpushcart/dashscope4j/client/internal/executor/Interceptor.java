package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;

public interface Interceptor {

    interface Chain {

        DashscopeClient client();

        ApiRequest<?> request();

    }

}
