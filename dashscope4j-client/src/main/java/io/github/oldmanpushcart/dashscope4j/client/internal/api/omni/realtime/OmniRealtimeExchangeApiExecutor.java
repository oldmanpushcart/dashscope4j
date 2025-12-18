package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class OmniRealtimeExchangeApiExecutor {

    private final ExchangeApiExecutor exchangeApi;
    private final Function<OmniRealtimeClientEvent, String> encoder;
    private final Function<String, OmniRealtimeServerEvent> decoder;

    public OmniRealtimeExchangeApiExecutor(String ak, HttpClient http, ObjectMapper mapper) {
        this.exchangeApi = new ExchangeApiExecutor(ak, http);
        this.encoder = e -> JacksonJsonUtils.toJson(mapper, e);
        this.decoder = s -> JacksonJsonUtils.toObject(mapper, s, OmniRealtimeServerEvent.class);
    }

    public CompletionStage<Exchange<OmniRealtimeClientEvent>> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var createdF = new CompletableFuture<>();
        final var futureMap = new ConcurrentHashMap<Class<?>, CompletableFuture<?>>();
        return exchangeApi
                .newExchange(model.endpoint(), encoder, decoder, new OmniRealtimeExchange.Handler() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                        futureMap.put(OmniRealtimeSessionCreatedServerEvent.class, createdF);
                        handler.onOpen(exchange);
                    }

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {

                        if (data instanceof OmniRealtimeSessionCreatedServerEvent) {
                            Optional.ofNullable(futureMap.remove(OmniRealtimeSessionCreatedServerEvent.class))
                                    .orElseThrow(() -> new IllegalStateException(""))
                                    .complete(null);
                        }

                        if (data instanceof OmniRealtimeSessionUpdatedServerEvent) {
                            Optional.ofNullable(futureMap.remove(OmniRealtimeSessionUpdatedServerEvent.class))
                                    .orElseThrow(() -> new IllegalStateException(""))
                                    .complete(null);
                        }

                        return handler.onData(data);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        futureMap.forEach((clazz, future) -> future.completeExceptionally(ex));
                        futureMap.clear();
                        handler.onClosed(ex);
                    }

                })

                /*
                 * 等带会话创建
                 */
                .thenCompose(e -> createdF.thenApply(u -> e))

                /*
                 * 发起并等待会话更新
                 */
                .thenCompose(exchange -> {

                    final var updatedF = new CompletableFuture<>()
                            .whenComplete((u, ex) -> futureMap.remove(OmniRealtimeSessionUpdatedServerEvent.class));
                    if (null != futureMap.putIfAbsent(OmniRealtimeSessionUpdatedServerEvent.class, updatedF)) {
                        throw new IllegalStateException();
                    }

                    final var session = new OmniRealtimeSession(parameters);
                    final var event = new OmniRealtimeSessionUpdateClientEvent(StringUtils.uuid(), session);
                    return exchange.send(event)
                            .thenCompose(unused -> updatedF)
                            .thenApply(u -> exchange);
                })
                ;
    }

}
