package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.DEFAULT_REALTIME_PATH;

public record OmniRealtimeModel(
        String name,
        String path
) implements Model<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    public OmniRealtimeModel(String name) {
        this(name, String.format("%s?model=%s".formatted(DEFAULT_REALTIME_PATH, name)));
    }

    public static final OmniRealtimeModel QWEN3_OMNI_FLASH_REALTIME = new OmniRealtimeModel("qwen3-omni-flash-realtime");

}
