package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpBuilderImpl;

import java.net.http.HttpClient;

public class OmniOpImpl implements OmniOp {

    private final String ak;
    private final HttpClient http;

    public OmniOpImpl(String ak, HttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @Override
    public OmniRealtimeOp realtime() {
        return OmniRealtimeOp.newOpBuilder()
                .ak(ak)
                .http(http)
                .build();
    }

    public static class BuilderImpl extends OpBuilderImpl<OmniOp, Builder> implements OmniOp.Builder {

        @Override
        public OmniOp build() {
            final var ak = ak();
            final var http = http();
            return new OmniOpImpl(ak, http);
        }

    }

}
