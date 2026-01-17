package io.github.oldmanpushcart.dashscope4j.client.internal.omni;

import io.github.oldmanpushcart.dashscope4j.client.omni.OmniOp;
import io.github.oldmanpushcart.dashscope4j.client.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.omni.realtime.OmniRealtimeOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;

public class OmniOpImpl implements OmniOp {

    private final OmniRealtimeOp omniRealtimeOp;

    public OmniOpImpl(String host, ExchangeApi exchangeApi) {
        this.omniRealtimeOp = new OmniRealtimeOpImpl(host, exchangeApi);
    }

    @Override
    public OmniRealtimeOp realtime() {
        return omniRealtimeOp;
    }

}
