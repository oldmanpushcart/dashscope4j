package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoModel;

import java.util.Map;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.DEFAULT_REALTIME_PATH;

public class OmniRealtimeModel extends AlgoModel {

    public OmniRealtimeModel(String name, Map<String, String> features) {
        super(name, String.format("%s?model=%s".formatted(DEFAULT_REALTIME_PATH, name)), features);
    }

    public OmniRealtimeModel(String name) {
        this(name, Map.of());
    }

    public static final OmniRealtimeModel QWEN3_OMNI_FLASH_REALTIME = new OmniRealtimeModel("qwen3-omni-flash-realtime");


}
