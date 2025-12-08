package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeOp;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.OpBuilderImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final ExchangeApiExecutor exchangeApi;
    private final Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> codec;

    public OmniRealtimeOpImpl(ExchangeApiExecutor exchangeApi, ObjectMapper mapper) {
        this.exchangeApi = exchangeApi;
        this.codec = new CodecImpl(mapper);
    }

    @Override
    public CompletionStage<OmniRealtimeExchange> newExchange(OmniRealtimeModel model, Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
        final var exchangeImplF = new CompletableFuture<OmniRealtimeExchangeImpl>();
        return exchangeApi
                .newExchange(model.endpoint(), codec, new Exchange.Handler<>() {

                    private volatile OmniRealtimeExchangeImpl exchangeImpl;

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin) {
                        this.exchangeImpl = new OmniRealtimeExchangeImpl(origin);
                        exchangeImplF.thenAccept(impl -> {
                            handler.onOpen(impl);
                        });
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {

                        if (event instanceof OmniRealtimeSessionCreatedServerEvent sessionCreatedEvent) {
                            final var parameters = sessionCreatedEvent.session();
                            exchangeImpl.updateParameters(parameters);
                            if (!exchangeImplF.complete(exchangeImpl)) {
                                final var dupSessionCreatedEx = new IllegalStateException("Duplicate session created!");
                                return CompletableFuture.failedStage(dupSessionCreatedEx);
                            }
                        }

                        if (event instanceof OmniRealtimeSessionUpdatedServerEvent sessionUpdatedEvent) {
                            final var parameters = sessionUpdatedEvent.session();
                            exchangeImpl.updateParameters(parameters);
                        }

                        return handler.onData(event);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public CompletionStage<Void> onClosed(Throwable ex) {
                        return handler.onClosed(ex);
                    }

                })
                .thenCompose(unused -> exchangeImplF)
                .thenApply(Function.identity());
    }


    private static class CodecImpl implements Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

        private final ObjectMapper mapper;

        private CodecImpl(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public String encode(OmniRealtimeClientEvent event) {
            return JacksonJsonUtils.toJson(event);
        }

        @Override
        public OmniRealtimeServerEvent decode(String json) {
            return JacksonJsonUtils.toObject(mapper, json, OmniRealtimeServerEvent.class);
        }

    }

    public static class BuilderImpl
            extends OpBuilderImpl<OmniRealtimeOp, OmniRealtimeOp.Builder>
            implements OmniRealtimeOp.Builder {

        private final ObjectMapper mapper = JacksonJsonUtils.newMapper();

        @Override
        public Builder registerServerEventSubType(String subname, Class<?> subtype) {
            mapper.registerSubtypes(new NamedType(subtype, subname));
            return this;
        }

        @Override
        public OmniRealtimeOp build() {
            final var ak = ak();
            final var http = http();
            final var exchangeApi = new ExchangeApiExecutor(ak, http);
            return new OmniRealtimeOpImpl(exchangeApi, mapper);
        }

    }

}
