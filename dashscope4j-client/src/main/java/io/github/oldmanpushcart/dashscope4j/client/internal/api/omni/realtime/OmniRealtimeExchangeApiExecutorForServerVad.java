package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeBufferAppendImageClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;

import java.awt.image.BufferedImage;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class OmniRealtimeExchangeApiExecutorForServerVad {

    private final OmniRealtimeExchangeApiExecutor exchangeApi;

    public OmniRealtimeExchangeApiExecutorForServerVad(String ak, HttpClient http, ObjectMapper mapper) {
        exchangeApi = new OmniRealtimeExchangeApiExecutor(ak, http, mapper);
    }

    public CompletionStage<OmniRealtimeExchange.ServerVad> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        return exchangeApi
                .newExchange(parameters, model, handler)
                .thenApply(ServerVad::new);
    }

    private static class ServerVad extends Exchange.Proxy<OmniRealtimeClientEvent> implements OmniRealtimeExchange.ServerVad {

        private final Exchange<OmniRealtimeClientEvent> origin;

        private ServerVad(Exchange<OmniRealtimeClientEvent> origin) {
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
