package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.HttpWsExchangeExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OmniRealtimeExchangeImpl implements OmniRealtimeExchange {

    private final Exchange<String, String> origin;
    private final ObjectMapper mapper;

    private final SessionOp sessionOp = new SessionOpImpl();
    private final BufferOp bufferOp = new BufferOpImpl();
    private final ResponseOp responseOp = new ResponseOpImpl();

    public OmniRealtimeExchangeImpl(Exchange<String, String> origin, ObjectMapper mapper) {
        this.origin = origin;
        this.mapper = mapper;
    }

    @Override
    public CompletionStage<Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent>> open(Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
        return origin
                .open(new Handler<>() {
                    @Override
                    public void onOpen(Exchange<String, String> exchange) {
                        handler.onOpen(OmniRealtimeExchangeImpl.this);
                    }

                    @Override
                    public CompletionStage<Void> onData(String data) {
                        final OmniRealtimeServerEvent event;
                        try {
                            event = mapper.reader().readValue(data, OmniRealtimeServerEvent.class);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to parse ServerEvent json!", e);
                        }
                        return handler.onData(event);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return CompletableFuture.completedStage(null);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        return handler.onClosed(ex);
                    }
                })
                .thenApply(v -> this);
    }

    @Override
    public boolean isClosed() {
        return origin.isClosed();
    }

    @Override
    public CompletionStage<Void> close() {
        return origin.close();
    }

    @Override
    public CompletionStage<Void> send(OmniRealtimeClientEvent data) {
        return proxySend(data);
    }

    @Override
    public CompletionStage<Void> send(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public SessionOp session() {
        return sessionOp;
    }

    @Override
    public ResponseOp response() {
        return responseOp;
    }

    @Override
    public BufferOp buffer() {
        return bufferOp;
    }

    private String genEventId() {
        return UUID.randomUUID().toString();
    }

    private CompletionStage<Void> proxySend(OmniRealtimeClientEvent event) {
        final var body = JacksonJsonUtils.toJson(event);
        return origin.send(body);
    }

    private class SessionOpImpl implements SessionOp {

        @Override
        public CompletionStage<Void> update(Parameters parameters) {
            return proxySend(new OmniRealtimeSessionUpdateClientEvent(
                    genEventId(),
                    new OmniRealtimeSession(parameters)
            ));
        }

    }

    private class BufferOpImpl implements BufferOp {

        @Override
        public CompletionStage<Void> append(BufferedImage image) {
            return proxySend(new OmniRealtimeBufferAppendImageClientEvent(genEventId(), image));
        }

        @Override
        public CompletionStage<Void> append(ByteBuffer buffer) {
            return proxySend(new OmniRealtimeBufferAppendAudioClientEvent(genEventId(), buffer));
        }

        @Override
        public CompletionStage<Void> commit() {
            return proxySend(new OmniRealtimeBufferCommitClientEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> clear() {
            return proxySend(new OmniRealtimeBufferClearClientEvent(genEventId()));
        }
    }

    private class ResponseOpImpl implements ResponseOp {

        @Override
        public CompletionStage<Void> create() {
            return proxySend(new OmniRealtimeResponseCreateClientEvent(genEventId()));
        }

        @Override
        public CompletionStage<Void> cancel() {
            return proxySend(new OmniRealtimeResponseCancelClientEvent(genEventId()));
        }

    }

    public static class BuilderImpl implements Builder {

        private String ak;
        private HttpClient http;
        private OmniRealtimeModel model;
        private final ObjectMapper mapper = JacksonJsonUtils.newMapper();

        @Override
        public Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = http;
            return this;
        }

        @Override
        public Builder model(OmniRealtimeModel model) {
            this.model = model;
            return this;
        }

        @Override
        public Builder registerServerEventSubType(String subtype, Class<? extends OmniRealtimeServerEvent> subclass) {
            mapper.registerSubtypes(new NamedType(subclass, subtype));
            return this;
        }

        @Override
        public OmniRealtimeExchange build() {
            final var origin = new HttpWsExchangeExecutor(ak, http)
                    .newExchange(model.endpoint(), Function.identity(), Function.identity());
            return new OmniRealtimeExchangeImpl(origin, mapper);
        }

    }

}
