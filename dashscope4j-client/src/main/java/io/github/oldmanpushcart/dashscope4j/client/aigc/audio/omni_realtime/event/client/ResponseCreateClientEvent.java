package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

public class ResponseCreateClientEvent extends ClientEvent {

    public ResponseCreateClientEvent(String id) {
        super(id, "response.create");
    }

}
