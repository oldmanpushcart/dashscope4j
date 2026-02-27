package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts_realtime.event.client;

public class SessionFinishClientEvent extends ClientEvent {

    public SessionFinishClientEvent(String id) {
        super(id, "session.finish");
    }

}
