package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.BaseOpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final OmniRealtimeExchangeApiExecutorForManualVad manualApi;
    private final OmniRealtimeExchangeApiExecutorForServerVad serverApi;

    private OmniRealtimeOpImpl(String ak, HttpClient http, ObjectMapper mapper) {
        this.manualApi = new OmniRealtimeExchangeApiExecutorForManualVad(ak, http, mapper);
        this.serverApi = new OmniRealtimeExchangeApiExecutorForServerVad(ak, http, mapper);
    }

    @Override
    public CompletionStage<ManualVad> newManualVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        return manualApi.newExchange(parameters, model, handler);
    }

    @Override
    public CompletionStage<ServerVad> newServerVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        return serverApi.newExchange(parameters, model, handler);
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
