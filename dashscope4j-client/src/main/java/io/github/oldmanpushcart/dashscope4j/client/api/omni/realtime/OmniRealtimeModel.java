package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.AlgoModel;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.common.Constants;

import java.net.URI;
import java.util.Map;

public class OmniRealtimeModel extends AlgoModel {

    public OmniRealtimeModel(String name) {
        this(Constants.REALTIME_ENDPOINT, name, new Parameters());
    }

    public OmniRealtimeModel(String name, Parameters parameters) {
        this(Constants.REALTIME_ENDPOINT, name, parameters);
    }

    private OmniRealtimeModel(URI endpoint, String name, Parameters parameters) {
        super(name, endpoint(endpoint, name), parameters);
    }

    private static URI endpoint(URI endpoint, String name) {
        return EndpointUtils.appendQueryParams(endpoint, Map.of("model", name));
    }

    public static final OmniRealtimeModel QWEN3_OMNI_FLASH_REALTIME = new OmniRealtimeModel("qwen3-omni-flash-realtime");

}
