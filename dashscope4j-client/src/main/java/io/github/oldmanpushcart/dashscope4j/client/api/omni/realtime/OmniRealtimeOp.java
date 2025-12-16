package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.OmniRealtimeOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.util.OpBuildable;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<OmniRealtimeExchange.Manual> newManual(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Manual.Handler handler);

    CompletionStage<OmniRealtimeExchange.Vad> newVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Vad.Handler handler);

    static OpBuilder newOpBuilder() {
        return new OmniRealtimeOpImpl.OpBuilderImpl();
    }

    interface OpBuilder extends OpBuildable<OmniRealtimeOp, OpBuilder> {

        OpBuilder registerServerEventSubType(String subname, Class<?> subtype);

    }

}
