package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys.TurnDetection;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.internal.BaseOpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final ExchangeApiExecutor exchangeApi;
    private final Function<OmniRealtimeClientEvent, String> encoder;
    private final Function<String, OmniRealtimeServerEvent> decoder;


    private OmniRealtimeOpImpl(String ak, HttpClient http, ObjectMapper mapper) {
        this.exchangeApi = new ExchangeApiExecutor(ak, http);
        this.encoder = e -> JacksonJsonUtils.toJson(mapper, e);
        this.decoder = s -> JacksonJsonUtils.toObject(mapper, s, OmniRealtimeServerEvent.class);
    }

    private static Parameters adjust(Parameters parameters, TurnDetection.Type tuneDetectionType) {
        final var newParameters = new Parameters().merge(parameters);
        final var newTurnDetection = Optional.ofNullable(parameters.get(OmniRealtimeParameterKeys.TURN_DETECTION))
                .map(turnDetection -> new TurnDetection(
                        tuneDetectionType,
                        turnDetection.threshold(),
                        turnDetection.silence()
                ))
                .orElseGet(() -> new TurnDetection(
                        tuneDetectionType,
                        null,
                        null
                ));
        newParameters.append(OmniRealtimeParameterKeys.TURN_DETECTION, newTurnDetection);
        return newParameters;
    }

    @Override
    public CompletionStage<ManualVad> newManualVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var endpoint = model.endpoint();
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.MANUAL_VAD);
        final var manualVadHandler = new ManualVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, manualVadHandler);
        return exchangeApi.newExchange(endpoint, encoder, decoder, handshakeHandler)
                .thenCompose(unused -> manualVadHandler.completeStage());
    }

    @Override
    public CompletionStage<ServerVad> newServerVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var endpoint = model.endpoint();
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.SERVER_VAD);
        final var serverVadHandler = new ServerVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, serverVadHandler);
        return exchangeApi.newExchange(endpoint, encoder, decoder, handshakeHandler)
                .thenCompose(unused -> serverVadHandler.completeStage());
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
