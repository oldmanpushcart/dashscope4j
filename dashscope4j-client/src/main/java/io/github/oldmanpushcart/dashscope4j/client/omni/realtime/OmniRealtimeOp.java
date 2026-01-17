package io.github.oldmanpushcart.dashscope4j.client.omni.realtime;

import io.github.oldmanpushcart.dashscope4j.client.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.omni.realtime.OmniRealtimeExchange.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.omni.realtime.OmniRealtimeExchange.ServerVad;

import java.util.concurrent.CompletionStage;

public interface OmniRealtimeOp {

    CompletionStage<ManualVad> newManualVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler);

    CompletionStage<ServerVad> newServerVad(OmniRealtimeModel model, Parameters parameters, OmniRealtimeExchange.Handler handler);

}
