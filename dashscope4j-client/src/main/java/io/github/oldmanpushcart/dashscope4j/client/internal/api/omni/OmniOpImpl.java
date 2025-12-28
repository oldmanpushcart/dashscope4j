package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.OmniRealtimeOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;

public class OmniOpImpl implements OmniOp {

    private final OmniRealtimeOp omniRealtimeOp;

    public OmniOpImpl(DashscopeClient client, ExchangeApi exchangeApi) {
        this.omniRealtimeOp = new OmniRealtimeOpImpl(client, exchangeApi);
    }

    @Override
    public OmniRealtimeOp realtime() {
        return omniRealtimeOp;
    }

}
