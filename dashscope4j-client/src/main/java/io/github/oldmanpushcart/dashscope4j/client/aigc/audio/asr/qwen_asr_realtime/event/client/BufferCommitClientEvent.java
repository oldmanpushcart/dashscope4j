package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client;

public class BufferCommitClientEvent extends ClientEvent {

    public BufferCommitClientEvent(String id) {
        super(id, "input_audio_buffer.commit");
    }

}
