package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeConnector;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeServerEvent;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class OmniRealtimeExchangeConnector extends RealtimeConnector<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    private OmniRealtimeExchangeConnector(Builder builder) {
        super(builder);
    }

    private static class CodecImpl implements Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

        @Override
        public String encode(OmniRealtimeClientEvent data) {
            return JacksonJsonUtils.toJson(OmniRealtimeClientEvent.class, data);
        }

        @Override
        public OmniRealtimeServerEvent decode(String json) {
            return JacksonJsonUtils.toObject(json, OmniRealtimeServerEvent.class);
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends RealtimeConnector.Builder<OmniRealtimeClientEvent, OmniRealtimeServerEvent, OmniRealtimeExchangeConnector, Builder> {

        private OmniRealtimeModel model;
        private OmniRealtimeSession session = OmniRealtimeSession.newBuilder().build();
        private DashscopeClient client;
        private Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> codec;
        private Supplier<? extends Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent>> handlerFactory;

        public Builder model(OmniRealtimeModel model) {
            this.model = model;
            return this;
        }

        public Builder session(OmniRealtimeSession session) {
            this.session = session;
            return this;
        }

        @Override
        public Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        @Override
        public Builder codec(Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> codec) {
            this.codec = codec;
            return this;
        }

        @Override
        public Builder handlerFactory(Supplier<? extends Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent>> handlerFactory) {
            this.handlerFactory = handlerFactory;
            return this;
        }

        @Override
        public OmniRealtimeExchangeConnector build() {

            requireNonNull(model, "model must not be null!");
            requireNonNull(client, "client must not be null!");
            requireNonNull(handlerFactory, "handlerFactory must not be null!");

            super.client(client);
            super.model(model);
            super.codec(null == codec ? new CodecImpl() : codec);
            super.handlerFactory(() -> {
                final var handler = handlerFactory.get();
                requireNonNull(handler, "handler from factory must not be null!");
                if (null == session
                        || null == session.turnDetection()
                        || OmniRealtimeSession.TurnDetection.Type.SERVER_VAD == session.turnDetection().type()) {
                    return new SessionHandshakeHandler(session, new ServerVadHandler(handler));
                } else {
                    return new SessionHandshakeHandler(session, new ManualVadHandler(handler));
                }
            });

            return new OmniRealtimeExchangeConnector(this);
        }

    }

}
