package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

public interface OmniRealtimeExchangeHandler
        extends Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    default void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange) {
        onOpen((OmniRealtimeExchange) exchange);
    }

    void onOpen(OmniRealtimeExchange exchange);

}
