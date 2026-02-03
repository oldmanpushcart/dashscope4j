package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

public class QwenTtsRealtimeBufferClearClientEvent extends QwenTtsRealtimeClientEvent {

    public QwenTtsRealtimeBufferClearClientEvent(String id) {
        super(id, "input_text_buffer.clear");
    }

}
