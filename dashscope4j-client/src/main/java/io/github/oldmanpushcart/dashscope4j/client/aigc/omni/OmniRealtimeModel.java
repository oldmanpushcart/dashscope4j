package io.github.oldmanpushcart.dashscope4j.client.aigc.omni;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.realtime.RealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.client.OmniRealtimeClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.omni.event.server.OmniRealtimeServerEvent;

import java.util.function.BiFunction;

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
    public BiFunction<OmniRealtimeSession, Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent>, Realtime.Handler<String, String>> provider() {
        return (session, handler) ->
                new CodecHandler<>(
                        JacksonJsonUtils::toJson,
                        s -> JacksonJsonUtils.toObject(s, OmniRealtimeServerEvent.class),
                        new SessionHandshakeHandler(
                                session,
                                handlerFactory(session, handler)
                        )
                );
    }

    private Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handlerFactory(OmniRealtimeSession session, Realtime.Handler<OmniRealtimeClientEvent, OmniRealtimeServerEvent> handler) {
        if (null == session
                || null == session.turnDetection()
                || OmniRealtimeSession.TurnDetection.Type.SERVER_VAD == session.turnDetection().type()) {
            return new ServerVadHandler(handler);
        } else {
            return new ManualVadHandler(handler);
        }
    }

}
