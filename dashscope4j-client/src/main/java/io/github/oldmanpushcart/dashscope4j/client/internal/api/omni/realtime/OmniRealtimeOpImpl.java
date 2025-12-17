package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.internal.BaseOpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler.ManualVadOmniRealtimeConnectHandler;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler.ServerVadOmniRealtimeConnectHandler;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final ExchangeApiExecutor executor;
    private final ObjectMapper mapper;

    private OmniRealtimeOpImpl(String ak, HttpClient http, ObjectMapper mapper) {
        this.executor = new ExchangeApiExecutor(ak, http);
        this.mapper = mapper;
    }

    @Override
    public CompletionStage<OmniRealtimeExchange.ManualVad> newManual(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.ManualVad.Handler handler) {
        return executor.newExchange(
                model.endpoint(),
                e->JacksonJsonUtils.toJson(mapper, e),
                s->JacksonJsonUtils.toObject(mapper, s, OmniRealtimeServerEvent.class),
                new ManualVadOmniRealtimeConnectHandler(parameters, handler)
        );
    }

    @Override
    public CompletionStage<OmniRealtimeExchange.ServerVad> newVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.ServerVad.Handler handler) {
        return executor.newExchange(
                model.endpoint(),
                e->JacksonJsonUtils.toJson(mapper, e),
                s->JacksonJsonUtils.toObject(mapper, s, OmniRealtimeServerEvent.class),
                new ServerVadOmniRealtimeConnectHandler(parameters, handler)
        );
    }


    public static class OpBuilderImpl
            extends BaseOpBuilderImpl<OmniRealtimeOp, OpBuilder>
            implements OpBuilder {

        private final ObjectMapper mapper = JacksonJsonUtils.newMapper();

        @Override
        public OpBuilder registerServerEventSubType(String subname, Class<?> subtype) {
            mapper.registerSubtypes(new NamedType(subtype, subname));
            return this;
        }

        @Override
        public OmniRealtimeOp build() {
            final var ak = ak();
            final var http = http();
            return new OmniRealtimeOpImpl(ak, http, mapper);
        }

    }

}
