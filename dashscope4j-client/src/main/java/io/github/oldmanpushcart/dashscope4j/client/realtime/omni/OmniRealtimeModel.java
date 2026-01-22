package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.Model;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.DEFAULT_REALTIME_PATH;

public record OmniRealtimeModel(String name, String path) implements Model {

    public OmniRealtimeModel(String name) {
        this(name, String.format("%s?model=%s".formatted(DEFAULT_REALTIME_PATH, name)));
    }

    public static final OmniRealtimeModel QWEN3_OMNI_FLASH_REALTIME = new OmniRealtimeModel("qwen3-omni-flash-realtime");


}
