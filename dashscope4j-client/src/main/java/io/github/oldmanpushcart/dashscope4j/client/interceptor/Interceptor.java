package io.github.oldmanpushcart.dashscope4j.client.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

public interface Interceptor {

    interface Chain {

        DashscopeClient client();

        ApiRequest<?> request();

    }

}
