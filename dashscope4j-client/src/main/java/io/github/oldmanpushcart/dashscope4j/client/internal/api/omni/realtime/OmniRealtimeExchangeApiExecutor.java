package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeErrorException;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureSlot;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class OmniRealtimeExchangeApiExecutor {

    private final ExchangeApiExecutor exchangeApi;
    private final Function<OmniRealtimeClientEvent, String> encoder;
    private final Function<String, OmniRealtimeServerEvent> decoder;

    public OmniRealtimeExchangeApiExecutor(String ak, HttpClient http, ObjectMapper mapper) {
        this.exchangeApi = new ExchangeApiExecutor(ak, http);
        this.encoder = e -> JacksonJsonUtils.toJson(mapper, e);
        this.decoder = s -> JacksonJsonUtils.toObject(mapper, s, OmniRealtimeServerEvent.class);
    }

    private static final String KEY_SESSION_CREATED = "session.created";
    private static final String KEY_SESSION_UPDATED = "session.updated";

    public CompletionStage<Exchange<OmniRealtimeClientEvent>> newExchange(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler) {
        final var futureSlot = new FutureSlot<String>();
        final var createdF = futureSlot.acquire(KEY_SESSION_CREATED);
        return exchangeApi
                .newExchange(model.endpoint(), encoder, decoder, new OmniRealtimeExchange.Handler() {

                    @Override
                    public void onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
                        handler.onOpen(exchange);
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

                        futureSlot.complete(data.type());
                        return handler.onData(data);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        futureSlot.drain().forEach((k, f) -> f.completeExceptionally(ex));
                        handler.onClosed(ex);
                    }

                })

                /*
                 * 等带会话创建
                 */
                .thenCompose(e -> createdF.thenApply(u -> e))
                .whenComplete((v, ex) -> futureSlot.release(KEY_SESSION_CREATED, createdF))

                /*
                 * 发起并等待会话更新
                 */
                .thenCompose(exchange -> {
                    final var updatedF = futureSlot.acquire(KEY_SESSION_UPDATED);
                    final var session = new OmniRealtimeSession(parameters);
                    final var event = new OmniRealtimeSessionUpdateClientEvent(genUUID22(), session);
                    return exchange.send(event)
                            .thenCompose(unused -> updatedF)
                            .whenComplete((v, ex) -> futureSlot.release(KEY_SESSION_UPDATED, updatedF))
                            .thenApply(u -> exchange);
                })
                ;
    }

}
