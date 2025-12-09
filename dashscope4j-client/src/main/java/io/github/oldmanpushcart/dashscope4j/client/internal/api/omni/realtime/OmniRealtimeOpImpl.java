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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OmniRealtimeOpImpl implements OmniRealtimeOp {

    private final ExchangeApiExecutor exchangeApi;
    private final Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> codec;

    public OmniRealtimeOpImpl(ExchangeApiExecutor exchangeApi, ObjectMapper mapper) {
        this.exchangeApi = exchangeApi;
        this.codec = new CodecImpl(mapper);
    }

    @Override
    public CompletionStage<OmniRealtimeExchange> newExchange(OmniRealtimeModel model, Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
        final var exchangeFutureHandler = new OmniRealtimeExchangeFutureHandler(handler);
        return exchangeApi
                .newExchange(model.endpoint(), codec, exchangeFutureHandler)
                .thenCompose(unused -> exchangeFutureHandler.getFuture());
    }

    private record CodecImpl(ObjectMapper mapper)
            implements Exchange.Codec<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

        @Override
        public String encode(OmniRealtimeClientEvent event) {
            return JacksonJsonUtils.toJson(mapper, event);
        }

        @Override
        public OmniRealtimeServerEvent decode(String json) {
            return JacksonJsonUtils.toObject(mapper, json, OmniRealtimeServerEvent.class);
        }

    }

    private static class OmniRealtimeExchangeFutureHandler implements Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler;
        private final CompletableFuture<OmniRealtimeExchange> exchangeF = new CompletableFuture<>();
        private final AtomicBoolean closedFlag = new AtomicBoolean(false);
        private volatile OmniRealtimeExchangeImpl exchangeImpl;

        private OmniRealtimeExchangeFutureHandler(Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
            this.handler = handler;
        }

        public CompletionStage<OmniRealtimeExchange> getFuture() {
            return exchangeF;
        }

        private CompletionStage<Void> fireClosed(Throwable ex) {

            if (!closedFlag.compareAndSet(false, true)) {
                return CompletableFuture.completedStage(null);
            }

            if (null != exchangeImpl && !exchangeImpl.isClosed()) {
                exchangeImpl.close();
            }

            return CompletableFuture.completedStage(null)
                    .thenCompose(unused -> handler.onClosed(ex));
        }

        @Override
        public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> origin) {
            this.exchangeImpl = new OmniRealtimeExchangeImpl(origin);
            this.exchangeF
                    .thenCompose(unused -> exchangeImpl.getSessionRefFuture())
                    .thenAccept(sessionRef -> {
                        logger.debug("dashscope-client://omni/realtime opened. exchange={};", exchangeImpl.uuid());
                        handler.onOpen(exchangeImpl);
                    })
                    .exceptionallyCompose(this::fireClosed);
        }

        @Override
        public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {

            if (event instanceof OmniRealtimeSessionCreatedServerEvent sessionCreatedEvent) {
                final var session = sessionCreatedEvent.session();
                exchangeImpl.getSessionRefFuture().complete(new AtomicReference<>(session));
                exchangeF.complete(exchangeImpl);
            }

            if (event instanceof OmniRealtimeSessionUpdatedServerEvent sessionUpdatedEvent) {
                final var session = sessionUpdatedEvent.session();
                exchangeImpl.getSessionRefFuture().join().set(session);
            }

            return CompletableFuture.completedStage(event)
                    .thenCompose(handler::onData)
                    .exceptionallyCompose(this::fireClosed);
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return handler.onBinary(buffer);
        }

        @Override
        public CompletionStage<Void> onClosed(Throwable ex) {
            return fireClosed(ex);
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
