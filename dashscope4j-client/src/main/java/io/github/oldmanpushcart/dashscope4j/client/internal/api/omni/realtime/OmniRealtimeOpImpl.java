package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.BaseOpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler.ManualOmniRealtimeConnectHandler;
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
    public CompletionStage<OmniRealtimeExchange.Manual> newManual(OmniRealtimeModel model, OmniRealtimeExchange.Manual.Handler handler) {

        return executor.newExchange(model.endpoint(), new ManualOmniRealtimeConnectHandler(handler));
    }

    @Override
    public CompletionStage<OmniRealtimeExchange.Vad> newVad(OmniRealtimeModel model, OmniRealtimeExchange.Manual.Handler handler) {
        return null;
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
