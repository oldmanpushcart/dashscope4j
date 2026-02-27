package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

public class BufferClearClientEvent extends ClientEvent {

    public BufferClearClientEvent(String id) {
        super(id, "input_text_buffer.clear");
    }

}
