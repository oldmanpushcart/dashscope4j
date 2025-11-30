package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeModel;

import java.net.http.HttpClient;

public class OmniOpImpl implements OmniOp {

    private final String ak;
    private final HttpClient http;

    public OmniOpImpl(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @Override
    public OmniRealtimeExchange newRealtimeExchange(OmniRealtimeModel model) {
        return OmniRealtimeExchange.newBuilder()
                .ak(ak)
                .http(http)
                .model(model)
                .build();
    }


    public static class BuilderImpl implements OmniOp.Builder {

        private String ak;
        private HttpClient http;

        @Override
        public Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = http;
            return this;
        }

        @Override
        public OmniOp build() {
            return new OmniOpImpl(ak, http);
        }

    }

}
