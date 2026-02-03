package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

public class QwenTtsRealtimeSessionFinishClientEvent extends QwenTtsRealtimeClientEvent{

    public QwenTtsRealtimeSessionFinishClientEvent(String id) {
        super(id, "session.finish");
    }

}
