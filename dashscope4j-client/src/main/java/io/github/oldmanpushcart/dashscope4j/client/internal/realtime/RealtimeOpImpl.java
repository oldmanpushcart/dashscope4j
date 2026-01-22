package io.github.oldmanpushcart.dashscope4j.client.internal.realtime;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.realtime.omni.OmniRealtimeOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeOp;

public class RealtimeOpImpl implements RealtimeOp {

    private final OmniRealtimeOp omniRealtimeOp;

    public RealtimeOpImpl(DashscopeClient client) {
        this.omniRealtimeOp = new OmniRealtimeOpImpl(client);
    }

    @Override
    public OmniRealtimeOp omni() {
        return omniRealtimeOp;
    }

}
