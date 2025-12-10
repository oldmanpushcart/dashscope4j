package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.handler.OmniRealtimeExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.util.OpBuildable;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.OmniRealtimeOpImpl;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<OmniRealtimeExchange> newExchange(OmniRealtimeModel model, OmniRealtimeExchangeHandler handler);

    static OpBuilder newOpBuilder() {
        return new OmniRealtimeOpImpl.OpBuilderImpl();
    }

    interface OpBuilder extends OpBuildable<OmniRealtimeOp, OpBuilder> {

        OpBuilder registerServerEventSubType(String subname, Class<?> subtype);

    }

}
