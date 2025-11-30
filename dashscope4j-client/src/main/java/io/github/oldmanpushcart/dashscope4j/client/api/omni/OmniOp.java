package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.OmniOpImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;

public interface OmniOp {

    OmniRealtimeExchange newRealtimeExchange(OmniRealtimeModel model);

    static Builder newBuilder() {
        return new OmniOpImpl.BuilderImpl();
    }

    interface Builder extends Buildable<OmniOp, Builder> {

        Builder ak(String ak);

        Builder http(HttpClient http);

    }

}
