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

public abstract class OmniRealtimeConnectHandler<E extends Exchange<OmniRealtimeClientEvent>>
        implements Exchange.ConnectHandler<OmniRealtimeClientEvent, OmniRealtimeServerEvent, E> {

    private final Parameters parameters;
    private final Exchange.ConsumeHandler<OmniRealtimeClientEvent, OmniRealtimeServerEvent, E> consumer;
    private final CompletableFuture<Void> sessionCreatedFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> sessionUpdatedFuture = new CompletableFuture<>();

    protected OmniRealtimeConnectHandler(Parameters parameters, Exchange.ConsumeHandler<OmniRealtimeClientEvent, OmniRealtimeServerEvent, E> consumer) {
        this.parameters = parameters;
        this.consumer = consumer;
    }

    abstract protected CompletionStage<E> processOnConnect(Exchange<OmniRealtimeClientEvent> exchange);

    abstract protected CompletionStage<Void> processOnData(OmniRealtimeServerEvent event);

    abstract protected CompletionStage<Void> processOnBinary(ByteBuffer buffer);

    abstract protected CompletionStage<Void> processOnClose(Throwable ex);

    @Override
    public CompletionStage<E> onConnect(Exchange<OmniRealtimeClientEvent> exchange) {
        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(unused -> sessionCreatedFuture)
                .thenCompose(unused -> sessionUpdate(exchange))
                .thenCompose(unused -> sessionUpdatedFuture)
                .thenCompose(unused -> processOnConnect(exchange))
                .whenComplete((e, ex) -> {
                    if (null == ex) {
                        consumer.onOpen(e);
                    }
                });
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

        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(unused -> processOnData(event))
                .thenCompose(unused -> consumer.onData(event));
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(unused -> processOnBinary(buffer))
                .thenCompose(unused -> consumer.onBinary(buffer));
    }

    @Override
    public CompletionStage<Void> onClosed(Throwable ex) {
        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(unused -> processOnClose(ex))
                .thenCompose(unused -> consumer.onClosed(ex));
    }

}
