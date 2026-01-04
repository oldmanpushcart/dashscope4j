package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.OmniOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.BaseOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.DefaultAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.DefaultFlowApi;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils;

import java.net.http.HttpClient;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class DashscopeClientImpl implements DashscopeClient {

    private final String host;

    private final OmniOp omniOp;
    private final ChatOp chatOp;
    private final BaseOp baseOp;

    private DashscopeClientImpl(Builder builder) {
        this.host = requireNonBlankString(builder.host, "host must not be blank!");
        String ak = CheckUtils.requireNonBlankString(builder.ak, "ak must not be blank!");
        HttpClient http = requireNonNull(builder.http, "http must not be null!");

        final var asyncApi = new DefaultAsyncApi(host, ak, http);
        final var flowApi = new DefaultFlowApi(host, ak, http);
        final var exchangeApi = new ExchangeApi(ak, http);

        this.chatOp = new ChatOpImpl(this, asyncApi, flowApi);
        this.omniOp = new OmniOpImpl(host, exchangeApi);
        this.baseOp = new BaseOpImpl(asyncApi);

    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public ChatOp chat() {
        return chatOp;
    }

    @Override
    public OmniOp omni() {
        return omniOp;
    }

    @Override
    public BaseOp base() {
        return baseOp;
    }


    public static class Builder implements DashscopeClient.Builder {

        private String host = Constants.DEFAULT_HOST;
        private String ak;
        private HttpClient http;

        @Override
        public Builder host(String host) {
            this.host = requireNonBlankString(host, "host must not be blank!");
            return this;
        }

        @Override
        public Builder ak(String ak) {
            this.ak = requireNonBlankString(ak, "ak must not be blank!");
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = requireNonNull(http, "http must not be null!");
            return this;
        }

        @Override
        public DashscopeClient build() {
            return new DashscopeClientImpl(this);
        }

    }

}
