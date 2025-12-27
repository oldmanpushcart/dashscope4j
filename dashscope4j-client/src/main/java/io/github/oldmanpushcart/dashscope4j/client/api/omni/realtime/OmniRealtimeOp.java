package io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.api.omni.realtime.OmniRealtimeExchange.ServerVad;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<ManualVad> newManualVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler);

    CompletionStage<ServerVad> newServerVad(Parameters parameters, OmniRealtimeModel model, OmniRealtimeExchange.Handler handler);

}
