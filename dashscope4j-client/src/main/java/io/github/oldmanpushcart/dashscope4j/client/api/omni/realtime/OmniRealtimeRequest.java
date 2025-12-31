package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.ExchangeRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.util.function.Function;

public class OmniRealtimeRequest extends ExchangeRequest<OmniRealtimeModel, OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    /**
     * 构造请求
     *
     * @param builder 构建者
     */
    protected OmniRealtimeRequest(Builder builder) {
        super(builder);
    }

    @Override
    protected Function<OmniRealtimeClientEvent, String> encoder() {
        return JacksonJsonUtils::toJson;
    }

    @Override
    protected Function<String, OmniRealtimeServerEvent> decoder() {
        return json -> JacksonJsonUtils.toObject(json, OmniRealtimeServerEvent.class);
    }

    public static class Builder extends ExchangeRequest.Builder<OmniRealtimeModel, OmniRealtimeRequest, OmniRealtimeClientEvent, OmniRealtimeServerEvent, Builder> {

        @Override
        public OmniRealtimeRequest build() {
            return new OmniRealtimeRequest(this);
        }

    }

}
