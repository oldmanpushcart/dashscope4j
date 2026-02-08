package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client;

public class ResponseCancelClientEvent extends ClientEvent {

    public ResponseCancelClientEvent(String id) {
        super(id, "response.cancel");
    }

}
