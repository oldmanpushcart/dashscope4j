package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public interface OmniRealtimeExchange extends Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    SessionOp session();

    ResponseOp response();

    BufferOp buffer();

    interface SessionOp {

        CompletionStage<OmniRealtimeSession> get();
        CompletionStage<Void> update(Parameters parameters);

    }

    interface ResponseOp {

        CompletionStage<Void> create();

        CompletionStage<Void> cancel();

    }

    interface BufferOp {

        CompletionStage<Void> appendImage(BufferedImage image);

        CompletionStage<Void> appendAudio(ByteBuffer buffer);

        CompletionStage<Void> appendAudio(byte[] bytes, int offset, int length);

        CompletionStage<Void> commit();

        CompletionStage<Void> clear();

    }

}
