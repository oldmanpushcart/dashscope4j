package io.github.oldmanpushcart.dashscope4j.client.internal.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeParameterKeys.TurnDetection;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeServerEvent;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final DashscopeClient client;
    private final OmniRealtimeExchange.Codec codec;

    public OmniRealtimeOpImpl(DashscopeClient client) {
        this.client = client;
        this.codec = new OmniRealtimeExchange.Codec() {
            @Override
            public String encode(OmniRealtimeClientEvent data) {
                return JacksonJsonUtils.toJson(OmniRealtimeClientEvent.class, data);
            }

            @Override
            public OmniRealtimeServerEvent decode(String json) {
                return JacksonJsonUtils.toObject(json, OmniRealtimeServerEvent.class);
            }
        };
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
    public CompletionStage<ManualVad> newManualVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler) {
        final var endpoint = EndpointUtils.wss(client.base().api().host(), model.path());
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.MANUAL_VAD);
        final var manualVadHandler = new ManualVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, manualVadHandler);
        return client.base().api().newExchange(endpoint, codec, handshakeHandler)
                .thenCompose(unused -> manualVadHandler.completeStage());
    }

    @Override
    public CompletionStage<ServerVad> newServerVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler) {
        final var endpoint = EndpointUtils.wss(client.base().api().host(), model.path());
        final var parametersAdjusted = adjust(parameters, TurnDetection.Type.SERVER_VAD);
        final var serverVadHandler = new ServerVadHandler(handler);
        final var handshakeHandler = new SessionHandshakeHandler(parametersAdjusted, serverVadHandler);
        return client.base().api().newExchange(endpoint, codec, handshakeHandler)
                .thenCompose(unused -> serverVadHandler.completeStage());
    }

}
