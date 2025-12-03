package io.github.oldmanpushcart.dashscope4j.client.api.omni.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.event.server.*;
import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class OmniRealtimeExchangeHandler
        extends Exchange.HandlerAdapter<OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    private volatile Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange;

    @Override
    public void onOpen(Exchange<OmniRealtimeClientEvent, OmniRealtimeServerEvent> exchange) {
        super.onOpen(exchange);
        this.exchange = exchange;
    }

    @Override
    final public CompletionStage<Void> onData(OmniRealtimeServerEvent event) {

        if (event instanceof OmniRealtimeResponseCreatedServerEvent responseCreatedEvent) {
            return onResponseBegin(responseCreatedEvent.response().id());
        }

        if (event instanceof OmniRealtimeResponseTextDeltaServerEvent responseTextDeltaEvent) {
            return onResponseItemText(
                    responseTextDeltaEvent.responseId(),
                    responseTextDeltaEvent.itemId(),
                    new Index(responseTextDeltaEvent.outputIndex(), responseTextDeltaEvent.contentIndex()),
                    responseTextDeltaEvent.delta()
            );
        }

        if (event instanceof OmniRealtimeResponseAudioDeltaServerEvent responseAudioDeltaEvent) {
            return onResponseItemAudio(
                    responseAudioDeltaEvent.responseId(),
                    responseAudioDeltaEvent.itemId(),
                    new Index(responseAudioDeltaEvent.outputIndex(), responseAudioDeltaEvent.contentIndex()),
                    responseAudioDeltaEvent.delta()
            );
        }

        if (event instanceof OmniRealtimeResponseAudioTranscriptDeltaServerEvent responseAudioTranscriptDeltaEvent) {
            return onResponseItemText(
                    responseAudioTranscriptDeltaEvent.responseId(),
                    responseAudioTranscriptDeltaEvent.itemId(),
                    new Index(responseAudioTranscriptDeltaEvent.outputIndex(), responseAudioTranscriptDeltaEvent.contentIndex()),
                    responseAudioTranscriptDeltaEvent.delta()
            );
        }

        if (event instanceof OmniRealtimeResponseDoneServerEvent responseDoneEvent) {
            return onResponseEnd(
                    responseDoneEvent.response().id(),
                    responseDoneEvent.response().usage()
            );
        }

        if (event instanceof OmniRealtimeErrorServerEvent errorEvent) {
            final var ex = new RuntimeException("code=%s;message=%s;".formatted(
                    errorEvent.error().code(),
                    errorEvent.error().message()
            ));
            return onClosed(ex)
                    .whenComplete((unused, closeEx) -> exchange.close());
        }

        return CompletableFuture.completedStage(null);
    }

    abstract public CompletionStage<Void> onResponseBegin(String responseId);

    abstract public CompletionStage<Void> onResponseItemText(String responseId, String itemId, Index index, String delta);

    abstract public CompletionStage<Void> onResponseItemAudio(String responseId, String itemId, Index index, ByteBuffer delta);

    abstract public CompletionStage<Void> onResponseEnd(String responseId, Usage usage);

    public record Index(int output, int content) {

        public int hashCode() {
            return output + content;
        }

        public boolean equals(Object obj) {
            return obj instanceof Index index
                    && index.output == output
                    && index.content == content;
        }

        @Override
        public String toString() {
            return "/%d/%d".formatted(output, content);
        }

    }

}
