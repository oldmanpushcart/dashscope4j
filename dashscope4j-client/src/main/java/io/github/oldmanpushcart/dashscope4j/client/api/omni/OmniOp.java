package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.OpBuilder;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.OmniOpImpl;

public interface OmniOp {

    OmniRealtimeOp realtime();

    static Builder newBuilder() {
        return new OmniOpImpl.BuilderImpl();
    }

    interface Builder extends OpBuilder<OmniOp, Builder> {

    }

}
