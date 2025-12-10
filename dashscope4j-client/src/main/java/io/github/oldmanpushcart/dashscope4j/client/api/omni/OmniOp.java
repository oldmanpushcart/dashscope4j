package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.util.OpBuildable;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.OmniOpImpl;

public interface OmniOp {

    OmniRealtimeOp realtime();

    static OpBuilder newOpBuilder() {
        return new OmniOpImpl.OpBuilderImpl();
    }

    interface OpBuilder extends OpBuildable<OmniOp, OpBuilder> {

    }

}
