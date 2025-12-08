package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;

import java.net.http.HttpClient;

public class DashscopeClientImpl implements DashscopeClient {

    private final String ak;
    private final HttpClient http;

    public DashscopeClientImpl(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @Override
    public ChatOp chat() {
        return ChatOp.newBuilder()
                .ak(ak)
                .http(http)
                .build();
    }

    @Override
    public OmniOp omni() {
        return OmniOp.newBuilder()
                .ak(ak)
                .http(http)
                .build();
    }

}
