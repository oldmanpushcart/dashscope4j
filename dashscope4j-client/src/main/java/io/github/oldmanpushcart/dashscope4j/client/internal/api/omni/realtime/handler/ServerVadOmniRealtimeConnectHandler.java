package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ServerVadOmniRealtimeConnectHandler extends OmniRealtimeConnectHandler<OmniRealtimeExchange.ServerVad> {

    public ServerVadOmniRealtimeConnectHandler(Parameters parameters, OmniRealtimeExchange.ServerVad.Handler handler) {
        super(adjustParameters(parameters), handler);
    }

    private static Parameters adjustParameters(Parameters parameters) {

        final var turnDetection = Optional.ofNullable(parameters.get(OmniRealtimeParameterKeys.TURN_DETECTION))
                .map(v-> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.SERVER_VAD,
                        v.threshold(),
                        v.silence()
                ))
                .orElse(new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.SERVER_VAD,
                        null,
                        null
                ));
        parameters.append(OmniRealtimeParameterKeys.TURN_DETECTION, turnDetection);

        return parameters;
    }

    @Override
    protected CompletionStage<OmniRealtimeExchange.ServerVad> processOnConnect(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.completedStage(new ServerVadImpl(exchange));
    }

    @Override
    protected CompletionStage<Void> processOnData(OmniRealtimeServerEvent event) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    protected CompletionStage<Void> processOnBinary(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    protected CompletionStage<Void> processOnClose(Throwable ex) {
        return CompletableFuture.completedStage(null);
    }

    private static class ServerVadImpl implements OmniRealtimeExchange.ServerVad {

        private final Exchange<OmniRealtimeClientEvent> origin;

        private ServerVadImpl(Exchange<OmniRealtimeClientEvent> origin) {
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> image(BufferedImage image) {
            final var event = new OmniRealtimeBufferAppendImageClientEvent(StringUtils.uuid(), image);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(ByteBuffer buffer) {
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(StringUtils.uuid(), buffer);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
            final var buffer = ByteBuffer.wrap(bytes, offset, length);
            return audio(buffer);
        }

        @Override
        public String id() {
            return origin.id();
        }

        @Override
        public boolean isClosed() {
            return origin.isClosed();
        }

        @Override
        public CompletionStage<Void> closing() {
            return origin.closing();
        }

        @Override
        public void close() {
            origin.close();
        }

        @Override
        public CompletionStage<Void> send(OmniRealtimeClientEvent data) {
            return origin.send(data);
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            return origin.send(buffer);
        }

    }

}
