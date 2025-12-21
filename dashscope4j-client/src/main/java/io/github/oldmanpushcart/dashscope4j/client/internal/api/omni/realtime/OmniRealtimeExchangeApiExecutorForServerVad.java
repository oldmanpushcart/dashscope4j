package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeErrorException;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ServerVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class OmniRealtimeExchangeApiExecutorForServerVad {

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForServerVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    private static Parameters adjust(Parameters parameters) {
        final var newParameters = new Parameters().merge(parameters);
        final var newTurnDetection = Optional.ofNullable(parameters.get(OmniRealtimeParameterKeys.TURN_DETECTION))
                .map(turnDetection -> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.SERVER_VAD,
                        turnDetection.threshold(),
                        turnDetection.silence()
                ))
                .orElseGet(() -> new OmniRealtimeParameterKeys.TurnDetection(
                        OmniRealtimeParameterKeys.TurnDetection.Type.SERVER_VAD,
                        null,
                        null
                ));
        newParameters.append(OmniRealtimeParameterKeys.TURN_DETECTION, newTurnDetection);
        return newParameters;
    }

    public CompletionStage<ServerVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var futureHandler = new FutureHandler(handler);
        return exchangeApi
                .newExchange(adjust(parameters), model, futureHandler)
                .thenCompose(unused -> futureHandler.getFuture());
    }


    private static class FutureHandler implements OmniRealtimeExchange.Handler {

        private final ServerVad.Handler delegate;
        private final CompletableFuture<ServerVad> future = new CompletableFuture<>();

        private FutureHandler(ServerVad.Handler delegate) {
            this.delegate = delegate;
        }

        public CompletableFuture<ServerVad> getFuture() {
            return future;
        }

        @Override
        public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
            final var serverVad = new ServerVadImpl(exchange);
            delegate.onOpen(serverVad);
            future.complete(new ServerVadImpl(exchange));
        }

        @Override
        public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

            /*
             * 统一捕捉对错误信息
             * 任何的错误都是不可被接收，遇到则说明发生了预期外的操作，需要主动关闭连接等待排查
             */
            if (data instanceof OmniRealtimeErrorServerEvent errorEvent) {
                final var error = errorEvent.error();
                throw new OmniRealtimeErrorException(error.code(), error.message());
            }

            /*
             * 检查会话响应类型是否为: ServerVad
             */
            if (data instanceof OmniRealtimeSessionUpdatedServerEvent sessionUpdatedEvent) {
                final var session = sessionUpdatedEvent.session();
                final var turnDetection = session.parameters().get(OmniRealtimeParameterKeys.TURN_DETECTION);
                if (null != turnDetection
                        && turnDetection.type() != OmniRealtimeParameterKeys.TurnDetection.Type.SERVER_VAD) {
                    throw new IllegalStateException("Invalid turn detection type: %s".formatted(turnDetection.type()));
                }
            }

            return delegate.onData(data);
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return delegate.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            delegate.onClosed(ex);
        }

    }

    private static class ServerVadImpl extends Exchange.Proxy<OmniRealtimeClientEvent> implements ServerVad {

        private final Exchange<OmniRealtimeClientEvent> origin;

        private ServerVadImpl(Exchange<OmniRealtimeClientEvent> origin) {
            super(origin);
            this.origin = origin;
        }

        @Override
        public CompletionStage<Void> image(BufferedImage image) {
            final var event = new OmniRealtimeBufferAppendImageClientEvent(genUUID22(), image);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(ByteBuffer buffer) {
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.send(event);
        }

        @Override
        public CompletionStage<Void> audio(byte[] bytes, int offset, int length) {
            final var buffer = ByteBuffer.wrap(bytes, offset, length);
            final var event = new OmniRealtimeBufferAppendAudioClientEvent(genUUID22(), buffer);
            return origin.send(event);
        }

    }

}
