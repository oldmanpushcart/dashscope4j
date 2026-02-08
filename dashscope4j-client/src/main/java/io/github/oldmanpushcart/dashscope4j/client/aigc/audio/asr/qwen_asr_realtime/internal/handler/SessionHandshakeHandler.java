package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class SessionHandshakeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {

    }

    @Override
    public CompletionStage<Void> onData(ServerEvent output) {
        return null;
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return null;
    }

    @Override
    public void onClosed(Throwable ex) {

    }

}
