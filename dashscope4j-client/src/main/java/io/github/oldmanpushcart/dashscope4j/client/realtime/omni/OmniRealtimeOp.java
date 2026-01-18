package io.github.oldmanpushcart.dashscope4j.client.realtime.omni;

import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.realtime.omni.OmniRealtimeExchange.ServerVad;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<ManualVad> newManualVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler);

    CompletionStage<ServerVad> newServerVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler);

}
