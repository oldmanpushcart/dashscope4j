package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface OmniRealtimeExchange extends Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    SessionOp session();

    ResponseOp response();

    BufferOp buffer();

    interface SessionOp {

        OmniRealtimeSession get();

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
