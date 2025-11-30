package io.github.oldmanpushcart.dashscope4j.client.api.omni;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.OmniRealtimeExchangeImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface OmniRealtimeExchange extends Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    SessionOp session();

    ResponseOp response();

    BufferOp buffer();

    static Builder newBuilder() {
        return new OmniRealtimeExchangeImpl.BuilderImpl();
    }

    interface SessionOp {

        CompletionStage<Void> update(Parameters parameters);

    }

    interface ResponseOp {

        CompletionStage<Void> create();

        CompletionStage<Void> cancel();

    }

    interface BufferOp {

        CompletionStage<Void> append(BufferedImage image);

        CompletionStage<Void> append(ByteBuffer buffer);

        CompletionStage<Void> commit();

        CompletionStage<Void> clear();

    }

    interface Builder extends Buildable<OmniRealtimeExchange, Builder> {

        Builder ak(String ak);

        Builder http(HttpClient http);

        Builder model(OmniRealtimeModel model);

        Builder registerServerEventSubType(String subtype, Class<? extends OmniRealtimeServerEvent> subclass);

    }

}
