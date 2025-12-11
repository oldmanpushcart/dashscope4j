package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface OmniRealtimeExchange extends Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    interface Manual extends OmniRealtimeExchange {

        CompletionStage<BufferOp> newConversation();

        interface BufferOp {

            CompletionStage<BufferOp> image(BufferedImage image);

            CompletionStage<BufferOp> audio(ByteBuffer buffer);

            CompletionStage<BufferOp> audio(byte[] bytes, int offset, int length);

            CompletionStage<BufferOp> clear();

            CompletionStage<ResponseOp> commit();

        }

        interface ResponseOp {

            CompletableFuture<Void> create();

        }

    }

    interface VAD extends OmniRealtimeExchange {

        CompletionStage<Manual.BufferOp> image(BufferedImage image);

        CompletionStage<Manual.BufferOp> audio(ByteBuffer buffer);

        CompletionStage<Manual.BufferOp> audio(byte[] bytes, int offset, int length);

    }

}
