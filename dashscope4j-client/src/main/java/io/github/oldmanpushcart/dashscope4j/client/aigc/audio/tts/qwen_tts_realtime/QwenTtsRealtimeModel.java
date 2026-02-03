package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client.QwenTtsRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.server.QwenTtsRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

public record QwenTtsRealtimeModel(
        String name,
        String path
) implements Model<QwenTtsRealtimeClientEvent, QwenTtsRealtimeServerEvent> {
    
}
