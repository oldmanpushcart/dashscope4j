package io.github.oldmanpushcart.dashscope4j.client.realtime.omni.handler;

import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.*;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 简单的 OMNI-REALTIME 数据交换处理器
 */
public abstract class SimpleOmniRealtimeHandler implements Realtime.Handler<OmniRealtimeServerEvent, OmniRealtimeClientEvent> {

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

        final var responseId = data.id();

        // 应答开始
        if (data instanceof OmniRealtimeResponseCreatedServerEvent) {
            return onResponseCreated(responseId);
        }

        // 应答结束
        else if (data instanceof OmniRealtimeResponseDoneServerEvent event) {
            return onResponseFinished(responseId, event.response().status());
        }

        // 应答文本块
        else if (data instanceof OmniRealtimeResponseTextDeltaServerEvent event) {
            return onResponseTextDelta(responseId, event.delta());
        }

        // 应答文本块（多模态）
        else if (data instanceof OmniRealtimeResponseAudioTranscriptDeltaServerEvent event) {
            return onResponseTextDelta(responseId, event.delta());
        }

        // 应答音频块（多模态）
        else if (data instanceof OmniRealtimeResponseAudioDeltaServerEvent event) {
            return onResponseAudioDelta(responseId, event.delta());
        }

        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    abstract public CompletionStage<Void> onResponseTextDelta(String responseId, String delta);

    abstract public CompletionStage<Void> onResponseAudioDelta(String responseId, ByteBuffer delta);

    abstract public CompletionStage<Void> onResponseCreated(String responseId);

    abstract public CompletionStage<Void> onResponseFinished(String responseId, OmniRealtimeServerEvent.Status status);

}
