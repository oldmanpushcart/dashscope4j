package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.ExchangeApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.FutureSlot;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
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
                .newExchange(model.endpoint(), encoder, decoder, new Exchange.ProxyHandler<>(handler) {

                    @Override
                    public CompletionStage<Void> onData(OmniRealtimeServerEvent data) {
                        futureSlot.complete(data.type());
                        return handler.onData(data);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        futureSlot.drain().forEach((k, f) -> f.completeExceptionally(ex));
                        handler.onClosed(ex);
                    }

                })

                /*
                 * 等带会话创建
                 * OMNI-REALTIME 连接之后，第一个必定是会话创建事件。所以这里以收到会话创建事件作为 Exchange 创建完成标志。
                 */
                .thenCompose(e -> createdF.thenApply(u -> e))
                .whenComplete((v, ex) -> futureSlot.release(KEY_SESSION_CREATED, createdF))

                /*
                 * 发起并等待会话更新
                 * OMNI-REALTIME 连接建立之后，并不清楚当前会话类型是 Manual VAD 还是 Server VAD，
                 * 所以这里需要进行强制配置更新，以确保当前会话是以期待的类型进行
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
