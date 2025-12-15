package io.github.oldmanpushcart.dashscope4j.client.internal.api.omni.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.client.OmniRealtimeSessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.event.server.OmniRealtimeSessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.StringUtils.uuid;

public abstract class OmniRealtimeExchangeHandler<U>
        implements Exchange.Handler<Exchange<OmniRealtimeClientEvent>, U, OmniRealtimeServerEvent> {

    private final Parameters parameters;
    private final CompletableFuture<Void> sessionCreatedFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> sessionUpdatedFuture = new CompletableFuture<>();

    protected OmniRealtimeExchangeHandler(Parameters parameters) {
        this.parameters = parameters;
    }

    abstract protected CompletionStage<U> make(Exchange<OmniRealtimeClientEvent> exchange);

    @Override
    public CompletionStage<U> onOpen(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(unused -> sessionCreatedFuture)
                .thenCompose(unused -> sessionUpdate(exchange))
                .thenCompose(unused -> sessionUpdatedFuture)
                .thenCompose(unused -> make(exchange));
    }

    private CompletionStage<Void> sessionUpdate(Exchange<OmniRealtimeClientEvent> exchange) {
        final var session = new OmniRealtimeSession(parameters);
        final var event = new OmniRealtimeSessionUpdateClientEvent(uuid(), session);
        return exchange.send(event);
    }

    @Override
    public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {

        if (event instanceof OmniRealtimeSessionCreatedServerEvent) {
            sessionCreatedFuture.complete(null);
        }

        if (event instanceof OmniRealtimeSessionUpdatedServerEvent) {
            sessionUpdatedFuture.complete(null);
        }

        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Void> onClosed(Throwable ex) {
        return CompletableFuture.completedStage(null);
    }

}
