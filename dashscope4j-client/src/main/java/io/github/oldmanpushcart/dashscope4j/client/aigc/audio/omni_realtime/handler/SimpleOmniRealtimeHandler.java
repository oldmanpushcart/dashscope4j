package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.*;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * 简单的 OMNI-REALTIME 数据交换处理器
 */
public abstract class SimpleOmniRealtimeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    @Override
    public void onData(ServerEvent output) {

        final var responseId = output.id();

        // 应答开始
        if (output instanceof ResponseCreatedServerEvent) {
            onResponseCreated(responseId);
        }

        // 应答结束
        else if (output instanceof ResponseDoneServerEvent event) {
            onResponseFinished(responseId, event.response().status());
        }

        // 应答文本块
        else if (output instanceof ResponseTextDeltaServerEvent event) {
            onResponseTextDelta(responseId, event.delta());
        }

        // 应答文本块（多模态）
        else if (output instanceof ResponseAudioTranscriptDeltaServerEvent event) {
            onResponseTextDelta(responseId, event.delta());
        }

        // 应答音频块（多模态）
        else if (output instanceof ResponseAudioDeltaServerEvent event) {
            onResponseAudioDelta(responseId, event.delta());
        }

        CompletableFuture.completedStage(null);
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
    }

    abstract public void onResponseTextDelta(String responseId, String delta);

    abstract public void onResponseAudioDelta(String responseId, ByteBuffer delta);

    abstract public void onResponseCreated(String responseId);

    abstract public void onResponseFinished(String responseId, ServerEvent.Status status);

}
