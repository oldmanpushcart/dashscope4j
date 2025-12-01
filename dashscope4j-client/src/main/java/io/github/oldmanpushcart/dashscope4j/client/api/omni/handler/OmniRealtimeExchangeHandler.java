package io.github.oldmanpushcart.dashscope4j.client.api.omni.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public abstract class OmniRealtimeExchangeHandler
        extends Exchange.HandlerAdapter<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {


    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {
        return null;
    }

    abstract CompletionStage<Void> onResponseBegin(String responseId);

    abstract CompletionStage<Void> onResponseText(String responseId, String itemId, String text);

    abstract CompletionStage<Void> onResponseAudio(String responseId, String itemId, ByteBuffer buffer);

    abstract CompletionStage<Void> onResponseEnd(String responseId, Usage usage);

}
