package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.OpBuilder;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.OmniRealtimeOpImpl;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<OmniRealtimeExchange> newExchange(OmniRealtimeModel model, Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler);

    static Builder newOpBuilder() {
        return new OmniRealtimeOpImpl.BuilderImpl();
    }

    interface Builder extends OpBuilder<OmniRealtimeOp, Builder> {

        Builder registerServerEventSubType(String subname, Class<?> subtype);

    }

}
