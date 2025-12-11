package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.handler.OmniRealtimeExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.internal.BaseOpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final OmniRealtimeExchangeApiExecutor executor;

    private OmniRealtimeOpImpl(String ak, HttpClient http, ObjectMapper mapper) {
        this.executor = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    @Override
    public CompletionStage<OmniRealtimeExchange> newExchange(OmniRealtimeModel model, OmniRealtimeExchangeHandler handler) {
        return executor.newExchange(model, handler);
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
