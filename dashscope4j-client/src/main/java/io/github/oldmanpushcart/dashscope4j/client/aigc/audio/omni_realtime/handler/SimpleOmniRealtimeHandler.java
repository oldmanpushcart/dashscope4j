package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.*;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 简单的 OMNI-REALTIME 数据交换处理器
 */
public abstract class SimpleOmniRealtimeHandler implements Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent output) {

        final var responseId = output.id();

        // 应答开始
        if (output instanceof OmniRealtimeResponseCreatedServerEvent) {
            return onResponseCreated(responseId);
        }

        // 应答结束
        else if (output instanceof OmniRealtimeResponseDoneServerEvent event) {
            return onResponseFinished(responseId, event.response().status());
        }

        // 应答文本块
        else if (output instanceof OmniRealtimeResponseTextDeltaServerEvent event) {
            return onResponseTextDelta(responseId, event.delta());
        }

        // 应答文本块（多模态）
        else if (output instanceof OmniRealtimeResponseAudioTranscriptDeltaServerEvent event) {
            return onResponseTextDelta(responseId, event.delta());
        }

        // 应答音频块（多模态）
        else if (output instanceof OmniRealtimeResponseAudioDeltaServerEvent event) {
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
