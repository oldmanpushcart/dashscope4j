package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.CodecExchangeHandler;
import io.github.oldmanpushcart.dashscope4j.client.Exchange;
import io.github.oldmanpushcart.dashscope4j.client.Model;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.event.server.OmniRealtimeServerEvent;

import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.common.Constants.DEFAULT_REALTIME_PATH;

public record OmniRealtimeModel(
        String name,
        String path
) implements RealtimeModel<OmniRealtimeSession, OmniRealtimeClientEvent, OmniRealtimeServerEvent> {

    public OmniRealtimeModel(String name) {
        this(name, String.format("%s?model=%s".formatted(DEFAULT_REALTIME_PATH, name)));
    }

    public static final OmniRealtimeModel QWEN3_OMNI_FLASH_REALTIME = new OmniRealtimeModel("qwen3-omni-flash-realtime");


    @Override
    public BiFunction<OmniRealtimeSession, Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent>, Exchange.Handler<String, String>> provider() {
        return (session, handler) ->
                new CodecExchangeHandler<>(
                        JacksonJsonUtils::toJson,
                        s -> JacksonJsonUtils.toObject(s, OmniRealtimeServerEvent.class),
                        new SessionHandshakeHandler(
                                session,
                                handlerFactory(session, handler)
                        )
                );
    }

    private Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handlerFactory(OmniRealtimeSession session, Exchange.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
        if (null == session
                || null == session.turnDetection()
                || OmniRealtimeSession.TurnDetection.Type.SERVER_VAD == session.turnDetection().type()) {
            return new SessionHandshakeHandler(session, new ServerVadHandler(handler));
        } else {
            return new SessionHandshakeHandler(session, new ManualVadHandler(handler));
        }
    }

}
