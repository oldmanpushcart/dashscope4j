package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcOp;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.aigc.AigcOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.BaseOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.realtime.RealtimeOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeOp;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils;

import java.net.http.HttpClient;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class DashscopeClientImpl implements DashscopeClient {

    private final RealtimeOp realtimeOp;
    private final AigcOp aigcOp;
    private final BaseOp baseOp;

    private DashscopeClientImpl(Builder builder) {
        final var host = requireNonBlankString(builder.host, "host must not be blank!");
        final var ak = CheckUtils.requireNonBlankString(builder.ak, "ak must not be blank!");
        final var http = requireNonNull(builder.http, "http must not be null!");

        this.aigcOp = new AigcOpImpl(this);
        this.realtimeOp = new RealtimeOpImpl(this);
        this.baseOp = new BaseOpImpl(this, host, ak, http);

    }

    @Override
    public AigcOp aigc() {
        return aigcOp;
    }

    @Override
    public RealtimeOp realtime() {
        return realtimeOp;
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
