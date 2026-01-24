package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ServerVad;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<ManualVad> newManualVad(OmniRealtimeModel model, OmniRealtimeSession session, OmniRealtimeExchange.Handler handler);

    CompletionStage<ServerVad> newServerVad(OmniRealtimeModel model, OmniRealtimeSession session, OmniRealtimeExchange.Handler handler);

}
