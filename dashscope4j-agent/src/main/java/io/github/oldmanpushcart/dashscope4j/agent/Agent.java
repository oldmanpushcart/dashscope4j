package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;

public interface Agent {

    String name();

    String description();

    DashscopeClient client();

}
