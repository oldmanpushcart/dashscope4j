package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
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
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final DashscopeClient client;
    private final ExchangeApi exchangeApi;
    private final Function<OmniRealtimeClientEvent, String> encoder;
    private final Function<String, OmniRealtimeServerEvent> decoder;

    public OmniRealtimeOpImpl(DashscopeClient client, ExchangeApi exchangeApi) {
        this.client = client;
        this.exchangeApi = exchangeApi;
        this.encoder = JacksonJsonUtils::toJson;
        this.decoder = s -> JacksonJsonUtils.toObject(s, OmniRealtimeServerEvent.class);
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

    private URI toEndpoint(OmniRealtimeModel model) {
        final var host = client.host();
        return EndpointUtils.wss(host, model.path());
    }

    @Override
    public CompletionStage<ManualVad> newManualVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.MANUAL_VAD);
        final var manualVadHandler = new ManualVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, manualVadHandler);
        return exchangeApi.newExchange(toEndpoint(model), encoder, decoder, handshakeHandler)
                .thenCompose(unused -> manualVadHandler.completeStage());
    }

    @Override
    public CompletionStage<ServerVad> newServerVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.SERVER_VAD);
        final var serverVadHandler = new ServerVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, serverVadHandler);
        return exchangeApi.newExchange(toEndpoint(model), encoder, decoder, handshakeHandler)
                .thenCompose(unused -> serverVadHandler.completeStage());
    }

}
