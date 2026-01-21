package io.github.oldmanpushcart.dashscope4j.client;

public interface Interceptor {

    interface Chain {

        DashscopeClient client();

        ApiRequest<?> request();

    }

}
